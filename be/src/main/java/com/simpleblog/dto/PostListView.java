package com.simpleblog.dto;

import java.util.Arrays;

import com.simpleblog.exception.InvalidPostQueryException;

/** The {@code view} parameter of {@code GET /public/api/v1/posts}: whether each summary also carries the card data. */
public enum PostListView {

	LIST("list"), CARD("card");

	private final String value;

	PostListView(String value) {
		this.value = value;
	}

	/** The view with exactly this spec name; anything else is rejected. */
	public static PostListView of(String value) {
		return Arrays.stream(values())
			.filter((view) -> view.value.equals(value))
			.findFirst()
			.orElseThrow(() -> new InvalidPostQueryException("Unknown view: " + value));
	}

}
