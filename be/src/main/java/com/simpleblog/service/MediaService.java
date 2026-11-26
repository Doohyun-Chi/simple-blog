package com.simpleblog.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.simpleblog.component.ImageType;
import com.simpleblog.component.MediaSlug;
import com.simpleblog.component.MediaStorage;
import com.simpleblog.component.MediaUsage;
import com.simpleblog.component.YouTubeLink;
import com.simpleblog.dto.ImageUpload;
import com.simpleblog.dto.MediaFile;
import com.simpleblog.dto.MediaInfo;
import com.simpleblog.dto.MediaList;
import com.simpleblog.entity.Media;
import com.simpleblog.entity.MediaProvider;
import com.simpleblog.exception.InvalidMediaException;
import com.simpleblog.exception.MediaNotFoundException;
import com.simpleblog.exception.NotMediaOwnerException;
import com.simpleblog.exception.MediaSlugTakenException;
import com.simpleblog.exception.MediaSlugUnavailableException;
import com.simpleblog.exception.UnsupportedMediaFileException;
import com.simpleblog.repository.MediaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaService {

	private static final String YOUTUBE_SLUG_PREFIX = "youtube-";

	private final MediaRepository mediaRepository;

	private final MediaStorage storage;

	private final MediaSlug mediaSlug;

	private final MediaUsage mediaUsage;

	private final YouTubeLink youTube;

	private final Clock clock;

	public MediaService(MediaRepository mediaRepository, MediaStorage storage, MediaSlug mediaSlug, MediaUsage mediaUsage,
			YouTubeLink youTube, Clock clock) {
		this.mediaRepository = mediaRepository;
		this.storage = storage;
		this.mediaSlug = mediaSlug;
		this.mediaUsage = mediaUsage;
		this.youTube = youTube;
		this.clock = clock;
	}

	/**
	 * Stores an image on the server, internal or, when the upload says so, content. The type comes from the content. The
	 * slug is the one the upload asks for, or is made of the file name. Under a slug the writer already has, the file is
	 * replaced when the upload says so; the second element of the answer then tells it was not newly created.
	 */
	@Transactional
	public Registration uploadImage(long ownerId, ImageUpload upload) {
		MediaProvider provider = uploadProviderOf(upload.provider());
		if (upload.content().length == 0) {
			throw new InvalidMediaException("The file is empty");
		}
		ImageType type = ImageType.of(upload.content()).orElseThrow(() -> new UnsupportedMediaFileException("Not a PNG, JPEG, GIF or WebP image"));
		if (upload.slug() == null) {
			return new Registration(storeImage(ownerId, this.mediaSlug.of(upload.fileName()), provider, type, upload.content()), true);
		}
		String slug = this.mediaSlug.chosen(upload.slug());
		Optional<Media> existing = this.mediaRepository.findBySlug(slug);
		if (existing.isEmpty()) {
			return new Registration(storeImage(ownerId, slug, provider, type, upload.content()), true);
		}
		return new Registration(replaceImage(ownerId, existing.get(), upload.replace(), type, upload.content()), false);
	}

	/** Registers a YouTube video by reference. The second element tells whether it was newly registered. */
	public Registration registerVideo(long ownerId, String url) {
		String videoId = this.youTube.videoIdOf(url == null ? "" : url)
			.orElseThrow(() -> new InvalidMediaException("Not a YouTube video link"));
		String slug = YOUTUBE_SLUG_PREFIX + videoId;
		Optional<Media> existing = this.mediaRepository.findBySlug(slug);
		if (existing.isPresent()) {
			return new Registration(MediaInfo.from(existing.get()), false);
		}
		return saveVideo(ownerId, slug, videoId);
	}

	/** Every media item, newest first, without paging: only the editor's gallery asks, and rarely. */
	@Transactional(readOnly = true)
	public MediaList listOfMedias() {
		return new MediaList(this.mediaRepository.listOfMedias().stream().map(MediaInfo::from).toList());
	}

	public MediaFile file(String slug) {
		Media media = this.mediaRepository.findBySlug(slug).filter(Media::storesFile)
			.orElseThrow(() -> new MediaNotFoundException("No image with slug: " + slug));
		return new MediaFile(this.storage.load(media.location()), media.mimeType());
	}

	/**
	 * Deletes the writer's own media item whatever its provider or use: the row now and, for an image, its file once the
	 * transaction commits. Posts that still refer to it are not touched.
	 */
	@Transactional
	public void delete(long ownerId, String slug) {
		Media media = this.mediaRepository.findBySlug(slug).orElseThrow(() -> new MediaNotFoundException("No media with slug: " + slug));
		if (!media.ownedBy(ownerId)) {
			throw new NotMediaOwnerException(slug);
		}
		discard(media);
	}

	/**
	 * Deletes each of these content images that the owner uploaded and no post or page refers to any more: the row now,
	 * the file once the transaction commits. Unknown slugs, the library's internal images, videos, someone else's images
	 * and images still in use are left alone.
	 * Run it after the post that replaced them is saved, so its new content is what gets checked.
	 */
	@Transactional
	public void discardUnused(long ownerId, List<String> slugs) {
		slugs.stream()
			.distinct()
			.map(this.mediaRepository::findBySlug)
			.flatMap(Optional::stream)
			.filter((media) -> media.discardable() && media.ownedBy(ownerId) && !this.mediaUsage.inUse(media.slug()))
			.forEach(this::discard);
	}

	private MediaInfo storeImage(long ownerId, String slug, MediaProvider provider, ImageType type, byte[] content) {
		String stored = this.storage.save(content, type.extension());
		try {
			return MediaInfo.from(this.mediaRepository.save(Media.storedImage(slug, provider, type.mimeType(), stored, now(), ownerId)));
		}
		catch (DataIntegrityViolationException ex) {
			this.storage.remove(stored);
			throw new MediaSlugTakenException(slug);
		}
		catch (RuntimeException ex) {
			this.storage.remove(stored);
			throw ex;
		}
	}

	/** Swaps the file of the writer's own image for this one; the old file goes once the swap is committed. */
	private MediaInfo replaceImage(long ownerId, Media existing, boolean replace, ImageType type, byte[] content) {
		if (!existing.storesFile() || !existing.ownedBy(ownerId)) {
			throw new MediaSlugUnavailableException(existing.slug());
		}
		if (!replace) {
			throw new MediaSlugTakenException(existing.slug());
		}
		String old = existing.location();
		existing.replaceFile(type.mimeType(), this.storage.save(content, type.extension()), now());
		this.storage.removeAfterCommit(old);
		return MediaInfo.from(existing);
	}

	/** The provider an upload asks for: internal when it names none, otherwise one that stores a file. */
	private MediaProvider uploadProviderOf(String code) {
		if (code == null) {
			return MediaProvider.INTERNAL;
		}
		return MediaProvider.find(code)
			.filter(MediaProvider::storesFile)
			.orElseThrow(() -> new InvalidMediaException("provider must be internal or content"));
	}

	private void discard(Media media) {
		this.mediaRepository.delete(media);
		if (media.storesFile()) {
			this.storage.removeAfterCommit(media.location());
		}
	}

	private Registration saveVideo(long ownerId, String slug, String videoId) {
		Media video = Media.externalVideo(slug, this.youTube.canonicalOf(videoId), this.youTube.thumbnailOf(videoId), now(), ownerId);
		try {
			return new Registration(MediaInfo.from(this.mediaRepository.save(video)), true);
		}
		catch (DataIntegrityViolationException ex) {
			return new Registration(MediaInfo.from(this.mediaRepository.findBySlug(slug).orElseThrow(() -> ex)), false);
		}
	}

	private LocalDateTime now() {
		return LocalDateTime.now(this.clock.withZone(ZoneOffset.UTC));
	}

	/** The outcome of registering a video: the item and whether this call created it. */
	public record Registration(MediaInfo media, boolean created) {
	}

}
