package com.simpleblog.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.simpleblog.entity.Bookmark;

/** A bookmark as in be/apispec.yaml ({@code Bookmark}). {@code slug} is the stored form; the datetimes are the site's local time. */
public record BookmarkInfo(Long id, Long postId, String slug, String name, int order, LocalDateTime createdDate,
		LocalDateTime modifiedDate) {

	public static BookmarkInfo from(Bookmark bookmark, ZoneId siteZone) {
		return new BookmarkInfo(bookmark.id(), bookmark.postId(), bookmark.postSlug(), bookmark.name(), bookmark.sortOrder(),
				local(bookmark.createdAt(), siteZone), bookmark.modifiedAt() == null ? null : local(bookmark.modifiedAt(), siteZone));
	}

	private static LocalDateTime local(LocalDateTime utc, ZoneId siteZone) {
		return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(siteZone).toLocalDateTime();
	}

}
