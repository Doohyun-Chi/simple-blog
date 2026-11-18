package com.simpleblog.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.simpleblog.entity.Post;

/**
 * A post's body as in be/apispec.yaml. {@code slug} is the stored form (percent-encoded for non-ASCII).
 * {@code date} and {@code modified} are the site's local time with no offset, like the WordPress REST API's fields
 * of the same names; the table keeps them in UTC.
 */
public record PostBody(String title, String slug, String content, String status, LocalDateTime date,
		LocalDateTime modified) {

	public static PostBody from(Post post, ZoneId siteZone) {
		return new PostBody(post.title(), post.slug(), post.content(), post.status(), local(post.createdAt(), siteZone),
				local(post.modifiedAt(), siteZone));
	}

	private static LocalDateTime local(LocalDateTime utc, ZoneId siteZone) {
		return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(siteZone).toLocalDateTime();
	}

}
