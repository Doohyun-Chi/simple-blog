package com.simpleblog.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * One entry of the page list in be/apispec.yaml. TB_POST has no parent and no menu order, so every page is a top
 * level one and the order is by id.
 */
public record PageSummary(Long id, String title, String slug, Long parentId, int menuOrder,
		Instant publishedAt, Instant modifiedAt) {

	/** The summary as the query reads it, with the UTC times of the table; the content of the page is never loaded. */
	public PageSummary(Long id, String title, String slug, LocalDateTime createdAt, LocalDateTime modifiedAt) {
		this(id, title, slug, null, 0, createdAt.toInstant(ZoneOffset.UTC), modifiedAt.toInstant(ZoneOffset.UTC));
	}

}
