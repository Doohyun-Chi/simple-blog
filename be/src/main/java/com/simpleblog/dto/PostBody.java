package com.simpleblog.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

import com.simpleblog.entity.Post;

/**
 * A post's body as in be/apispec.yaml. {@code slug} is the stored form (percent-encoded for non-ASCII).
 * {@code date} and {@code modified} are the site's local time with no offset, like the WordPress REST API's fields
 * of the same names; the table keeps them in UTC. {@code hashTags} is the comma separated stored text, empty when there is none. {@code canEdit} tells whether the requester may update and delete it.
 */
public record PostBody(String title, String slug, String content, String status, String type, LocalDateTime date,
		LocalDateTime modified, String hashTags, boolean canEdit) {

	public static PostBody from(Post post, ZoneId siteZone, long viewerId) {
		return new PostBody(post.title(), post.slug(), post.content(), post.status(), post.type(), local(post.createdAt(), siteZone),
				local(post.modifiedAt(), siteZone), Objects.requireNonNullElse(post.hashTags(), ""), post.editableBy(viewerId));
	}

	private static LocalDateTime local(LocalDateTime utc, ZoneId siteZone) {
		return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(siteZone).toLocalDateTime();
	}

}
