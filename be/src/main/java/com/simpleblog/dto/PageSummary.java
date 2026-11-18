package com.simpleblog.dto;

import java.time.Instant;
import java.time.ZoneOffset;

import com.simpleblog.entity.Post;

/**
 * One entry of the page list in be/apispec.yaml. TB_POST has no parent and no menu order, so every page is a top
 * level one and the order is by id.
 */
public record PageSummary(Long id, String title, String slug, Long parentId, int menuOrder,
		Instant publishedAt, Instant modifiedAt) {

	public static PageSummary from(Post post) {
		return new PageSummary(post.id(), post.title(), post.slug(), null, 0,
				post.createdAt().toInstant(ZoneOffset.UTC), post.modifiedAt().toInstant(ZoneOffset.UTC));
	}

}
