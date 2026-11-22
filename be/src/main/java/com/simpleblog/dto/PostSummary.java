package com.simpleblog.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.simpleblog.entity.PostStatus;

/** One entry of the post list in be/apispec.yaml. {@code date} is the site's local time; the table keeps UTC. */
public record PostSummary(Long id, String title, String slug, PostStatus status, LocalDateTime date) {

	/** The summary as the query reads it, with the UTC creation time, in the site's local time. */
	public PostSummary inZone(ZoneId siteZone) {
		return new PostSummary(this.id, this.title, this.slug, this.status,
				this.date.atOffset(ZoneOffset.UTC).atZoneSameInstant(siteZone).toLocalDateTime());
	}

}
