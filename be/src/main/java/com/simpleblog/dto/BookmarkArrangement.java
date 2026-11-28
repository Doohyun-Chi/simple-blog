package com.simpleblog.dto;

import java.util.List;

/** The JSON body of the bookmark saving in be/apispec.yaml ({@code BookmarkArrangement}). */
public record BookmarkArrangement(List<Item> bookmarks) {

	/** The name and the order a bookmark is to have. */
	public record Item(Long id, String name, Integer order) {
	}

}
