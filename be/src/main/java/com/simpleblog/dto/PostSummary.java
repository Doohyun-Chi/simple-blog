package com.simpleblog.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.simpleblog.entity.PostStatus;

/**
 * One entry of the post list in be/apispec.yaml. {@code date} is the site's local time; the table keeps UTC. {@code thumbnail}
 * and {@code excerpt} are the card data, left out of the JSON unless the card view asked for them (and a post has an image).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostSummary(Long id, String title, String slug, PostStatus status, LocalDateTime date, String thumbnail, String excerpt) {

	/** A summary without card data. */
	public PostSummary(Long id, String title, String slug, PostStatus status, LocalDateTime date) {
		this(id, title, slug, status, date, null, null);
	}

	/** The summary as the query reads it, with the UTC creation time, in the site's local time. */
	public PostSummary inZone(ZoneId siteZone) {
		return new PostSummary(this.id, this.title, this.slug, this.status,
				this.date.atOffset(ZoneOffset.UTC).atZoneSameInstant(siteZone).toLocalDateTime(), this.thumbnail, this.excerpt);
	}

}
