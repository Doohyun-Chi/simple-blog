package com.simpleblog.dto;

import java.time.LocalDateTime;

import com.simpleblog.entity.PostStatus;
import com.simpleblog.entity.PostType;

/** A post with its content as in be/mcpapispec.yaml ({@code PostContent}). */
public record McpPostContent(String slug, String title, String content, PostStatus status, PostType type, LocalDateTime date,
		LocalDateTime modified, String hashTags) {

	public static McpPostContent from(PostBody body) {
		return new McpPostContent(body.slug(), body.title(), body.content(), body.status(), body.type(), body.date(), body.modified(),
				body.hashTags());
	}

}
