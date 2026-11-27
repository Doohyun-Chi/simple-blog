package com.simpleblog.dto;

import java.time.LocalDateTime;

import com.simpleblog.entity.PostStatus;

/** One post of a list tool as in be/mcpapispec.yaml ({@code PostSummaries}). */
public record McpPostSummary(String slug, String title, PostStatus status, LocalDateTime date) {

	public static McpPostSummary from(PostSummary summary) {
		return new McpPostSummary(summary.slug(), summary.title(), summary.status(), summary.date());
	}

}
