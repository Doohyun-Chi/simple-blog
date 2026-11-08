package com.simpleblog.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.simpleblog.entity.Post;

public record PageSummary(Long id, String title, String slug, Long parentId, int menuOrder,
		Instant publishedAt, Instant modifiedAt) {

	private static final long NO_PARENT = 0L;

	public static PageSummary from(Post post) {
		return new PageSummary(post.id(), post.title(), post.slug(), parentIdOf(post), post.menuOrder(),
				toUtcInstant(post.publishedAtGmt()), toUtcInstant(post.modifiedAtGmt()));
	}

	private static Long parentIdOf(Post post) {
		Long parentId = post.parentId();
		return (parentId == null || parentId == NO_PARENT) ? null : parentId;
	}

	private static Instant toUtcInstant(LocalDateTime gmt) {
		return gmt.toInstant(ZoneOffset.UTC);
	}

}
