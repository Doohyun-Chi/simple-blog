package com.simpleblog.dto;

import java.time.LocalDateTime;

import com.simpleblog.entity.Post;

/**
 * A post's body as in be/apispec.yaml. {@code date} and {@code modified} are the site's local time with no offset,
 * like the WordPress REST API's fields of the same names.
 */
public record PostBody(String title, String content, String status, LocalDateTime date, LocalDateTime modified) {

	public static PostBody from(Post post) {
		return new PostBody(post.title(), post.content(), post.status(), post.publishedAtLocal(),
				post.modifiedAtLocal());
	}

}
