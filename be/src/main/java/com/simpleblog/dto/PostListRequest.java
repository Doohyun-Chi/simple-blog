package com.simpleblog.dto;

/**
 * What {@code GET /public/api/v1/posts} asks for. {@code limit} is null for every match. {@code origin} is where this server is
 * reached, which completes a relative image source of a card.
 */
public record PostListRequest(PostQueryType queryType, String query, Integer limit, int offset, PostListView view, String origin) {

	/** The first {@code limit} posts matching {@code query}, as a plain list. */
	public static PostListRequest keyword(String query, Integer limit) {
		return new PostListRequest(PostQueryType.KEYWORD, query, limit, 0, PostListView.LIST, "");
	}

}
