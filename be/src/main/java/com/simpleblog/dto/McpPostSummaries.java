package com.simpleblog.dto;

import java.util.List;

/** The data of the list tools as in be/mcpapispec.yaml ({@code PostSummaries}). */
public record McpPostSummaries(List<McpPostSummary> posts) {

	public static McpPostSummaries from(PostList list) {
		return new McpPostSummaries(list.posts().stream().map(McpPostSummary::from).toList());
	}

}
