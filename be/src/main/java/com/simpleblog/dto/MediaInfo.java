package com.simpleblog.dto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.simpleblog.entity.Media;
import com.simpleblog.entity.MediaProvider;

/** A media item as in be/apispec.yaml ({@code Media}). {@code url} is a server-relative path for an image. */
public record MediaInfo(String slug, MediaProvider provider, String mimeType, String url, String thumbnail, OffsetDateTime date) {

	public static final String FILE_PATH = "/public/api/v1/media/";

	public static MediaInfo from(Media media) {
		String url = media.storesFile() ? FILE_PATH + media.slug() : media.location();
		return new MediaInfo(media.slug(), media.provider(), media.mimeType(), url, media.thumbnail(),
				media.date().atOffset(ZoneOffset.UTC));
	}

}
