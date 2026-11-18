package com.simpleblog.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import com.simpleblog.component.ImageType;
import com.simpleblog.component.MediaSlug;
import com.simpleblog.component.MediaStorage;
import com.simpleblog.component.YouTubeLink;
import com.simpleblog.dto.MediaFile;
import com.simpleblog.dto.MediaInfo;
import com.simpleblog.dto.MediaList;
import com.simpleblog.entity.Media;
import com.simpleblog.exception.InvalidMediaException;
import com.simpleblog.exception.MediaNotFoundException;
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

	private final YouTubeLink youTube;

	private final Clock clock;

	public MediaService(MediaRepository mediaRepository, MediaStorage storage, MediaSlug mediaSlug, YouTubeLink youTube, Clock clock) {
		this.mediaRepository = mediaRepository;
		this.storage = storage;
		this.mediaSlug = mediaSlug;
		this.youTube = youTube;
		this.clock = clock;
	}

	/** Stores an image on the server. The type comes from the content; the file name only seeds the slug. */
	public MediaInfo uploadImage(long ownerId, String fileName, byte[] content) {
		if (content.length == 0) {
			throw new InvalidMediaException("The file is empty");
		}
		ImageType type = ImageType.of(content).orElseThrow(() -> new UnsupportedMediaFileException("Not a PNG, JPEG, GIF or WebP image"));
		String stored = this.storage.save(content, type.extension());
		try {
			return MediaInfo.from(this.mediaRepository.save(
					Media.internalImage(this.mediaSlug.of(fileName), type.mimeType(), stored, now(), ownerId)));
		}
		catch (RuntimeException ex) {
			this.storage.remove(stored);
			throw ex;
		}
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
		Media media = this.mediaRepository.findBySlug(slug).filter(Media::isInternal)
			.orElseThrow(() -> new MediaNotFoundException("No image with slug: " + slug));
		return new MediaFile(this.storage.load(media.location()), media.mimeType());
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
