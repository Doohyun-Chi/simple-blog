package com.simpleblog.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
import com.simpleblog.dto.MediaSource;
import com.simpleblog.entity.Media;
import com.simpleblog.entity.MediaProvider;
import com.simpleblog.entity.SourceType;
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

	/** The largest source of an image, in UTF-8 bytes: 1 MB, as be/apispec.yaml says. */
	private static final int SOURCE_MAX_BYTES = 1024 * 1024;

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
	 * replaced when the upload says so; the second element of the answer then tells it was not newly created. A content
	 * image may come with its source, which is kept beside the file and replaced with it.
	 */
	@Transactional
	public Registration uploadImage(long ownerId, ImageUpload upload) {
		MediaProvider provider = uploadProviderOf(upload.provider());
		ImageFile image = imageOf(upload);
		if (upload.slug() == null) {
			return new Registration(storeImage(ownerId, this.mediaSlug.of(upload.fileName()), provider, image), true);
		}
		String slug = this.mediaSlug.chosen(upload.slug());
		Optional<Media> existing = this.mediaRepository.findBySlug(slug);
		if (existing.isEmpty()) {
			return new Registration(storeImage(ownerId, slug, provider, image), true);
		}
		return new Registration(replaceImage(ownerId, existing.get(), upload.replace(), image), false);
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
		return new MediaFile(this.storage.load(media.location()), media.mimeType(), media.location());
	}

	/** The source kept beside an image, to be served like its file; its stored name changes with it. */
	public MediaFile source(String slug) {
		Media media = withSource(slug);
		String name = sourceNameOf(media.location(), media.sourceType());
		return new MediaFile(this.storage.load(name), media.sourceType().mimeType(), name);
	}

	/** The source kept beside an image as text, for a reader that wants it in a message, such as an MCP tool. */
	public MediaSource sourceText(String slug) {
		Media media = withSource(slug);
		try {
			String text = this.storage.load(sourceNameOf(media.location(), media.sourceType())).getContentAsString(StandardCharsets.UTF_8);
			return new MediaSource(media.slug(), media.sourceType(), text);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read the source of the image " + slug, ex);
		}
	}

	private Media withSource(String slug) {
		return this.mediaRepository.findBySlug(slug).filter((media) -> media.sourceType() != null)
			.orElseThrow(() -> new MediaNotFoundException("No image source has the slug: " + slug));
	}

	/**
	 * Gives the writer's own image a new slug. Posts that show it by the old file URL are not changed. A video cannot be
	 * renamed, and the slug must be free.
	 */
	@Transactional
	public MediaInfo rename(long ownerId, String slug, String newSlug) {
		Media media = this.mediaRepository.findBySlug(slug).orElseThrow(() -> new MediaNotFoundException("No media with slug: " + slug));
		if (!media.ownedBy(ownerId)) {
			throw new NotMediaOwnerException(slug);
		}
		if (!media.storesFile()) {
			throw new MediaSlugUnavailableException(slug);
		}
		String chosen = this.mediaSlug.chosen(newSlug == null ? "" : newSlug);
		if (chosen.equals(slug)) {
			return MediaInfo.from(media);
		}
		if (this.mediaRepository.findBySlug(chosen).isPresent()) {
			throw new MediaSlugTakenException(chosen);
		}
		media.renameTo(chosen);
		return MediaInfo.from(media);
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

	/** The image of an upload, checked: its type comes from the content, and its source is there in full or not at all. */
	private ImageFile imageOf(ImageUpload upload) {
		if (upload.content().length == 0) {
			throw new InvalidMediaException("The file is empty");
		}
		ImageType type = ImageType.of(upload.content()).orElseThrow(() -> new UnsupportedMediaFileException("Not a PNG, JPEG, GIF or WebP image"));
		if (upload.source() == null && upload.sourceType() == null) {
			return new ImageFile(type, upload.content(), null, null);
		}
		return new ImageFile(type, upload.content(), sourceTypeOf(upload.sourceType()), sourceBytesOf(upload.source()));
	}

	private SourceType sourceTypeOf(String code) {
		if (code == null) {
			throw new InvalidMediaException("source must come with sourceType");
		}
		return SourceType.find(code).orElseThrow(() -> new InvalidMediaException("Unknown sourceType: " + code));
	}

	private byte[] sourceBytesOf(String source) {
		if (source == null || source.isEmpty()) {
			throw new InvalidMediaException("sourceType must come with a source that is not empty");
		}
		byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
		if (bytes.length > SOURCE_MAX_BYTES) {
			throw new InvalidMediaException("The source is over 1 MB");
		}
		return bytes;
	}

	/** Refuses a source for an image of a provider that keeps none, such as an image of the library. */
	private void requireSourceKept(MediaProvider provider, ImageFile image) {
		if (image.sourceType() != null && !provider.keepsSource()) {
			throw new InvalidMediaException("Only a content image keeps a source");
		}
	}

	private MediaInfo storeImage(long ownerId, String slug, MediaProvider provider, ImageFile image) {
		requireSourceKept(provider, image);
		String stored = storeFiles(image);
		try {
			Media media = Media.storedImage(slug, provider, image.type().mimeType(), stored, image.sourceType(), now(), ownerId);
			return MediaInfo.from(this.mediaRepository.save(media));
		}
		catch (DataIntegrityViolationException ex) {
			filesOf(stored, image.sourceType()).forEach(this.storage::remove);
			throw new MediaSlugTakenException(slug);
		}
		catch (RuntimeException ex) {
			filesOf(stored, image.sourceType()).forEach(this.storage::remove);
			throw ex;
		}
	}

	/** Swaps the file and the source of the writer's own image for these; the old ones go once the swap is committed. */
	private MediaInfo replaceImage(long ownerId, Media existing, boolean replace, ImageFile image) {
		if (!existing.storesFile() || !existing.ownedBy(ownerId)) {
			throw new MediaSlugUnavailableException(existing.slug());
		}
		if (!replace) {
			throw new MediaSlugTakenException(existing.slug());
		}
		requireSourceKept(existing.provider(), image);
		List<String> old = filesOf(existing.location(), existing.sourceType());
		existing.replaceFile(image.type().mimeType(), storeFiles(image), image.sourceType(), now());
		old.forEach(this.storage::removeAfterCommit);
		return MediaInfo.from(existing);
	}

	/** Writes the image file and its source, if any, beside it; returns the stored name of the image. */
	private String storeFiles(ImageFile image) {
		String stored = this.storage.save(image.content(), image.type().extension());
		if (image.sourceType() == null) {
			return stored;
		}
		try {
			this.storage.saveBeside(stored, image.sourceType().extension(), image.source());
			return stored;
		}
		catch (RuntimeException ex) {
			this.storage.remove(stored);
			throw ex;
		}
	}

	/** The files of a stored image: its own, and its source beside it when it has one. */
	private static List<String> filesOf(String stored, SourceType sourceType) {
		return sourceType == null ? List.of(stored) : List.of(stored, sourceNameOf(stored, sourceType));
	}

	private static String sourceNameOf(String stored, SourceType sourceType) {
		return MediaStorage.besideName(stored, sourceType.extension());
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
			filesOf(media.location(), media.sourceType()).forEach(this.storage::removeAfterCommit);
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

	/** An uploaded image: its type and bytes, and its source with the kind of it, or two nulls when it has none. */
	private record ImageFile(ImageType type, byte[] content, SourceType sourceType, byte[] source) {
	}

}
