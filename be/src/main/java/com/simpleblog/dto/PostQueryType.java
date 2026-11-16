package com.simpleblog.dto;

import java.util.Arrays;

import com.simpleblog.exception.InvalidPostQueryException;

/** The {@code queryType} parameter of {@code GET /public/api/v1/posts}. Values are the spec's lowercase names. */
public enum PostQueryType {

	KEYWORD("keyword"), TAG("tag");

	private final String value;

	PostQueryType(String value) {
		this.value = value;
	}

	/** The type with exactly this spec name; enum constant names such as KEYWORD are rejected like any other value. */
	public static PostQueryType of(String value) {
		return Arrays.stream(values())
			.filter((type) -> type.value.equals(value))
			.findFirst()
			.orElseThrow(() -> new InvalidPostQueryException("Unknown queryType: " + value));
	}

}
