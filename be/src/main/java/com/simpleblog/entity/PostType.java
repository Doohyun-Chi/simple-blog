package com.simpleblog.entity;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * {@code TB_POST.TYPE}: every row is a post or a page, no other type is stored. The code is the stored and the JSON form, and also the first segment of the reader's path.
 */
public enum PostType {

	POST("post"), PAGE("page");

	private final String code;

	PostType(String code) {
		this.code = code;
	}

	@JsonValue
	public String code() {
		return this.code;
	}

	/** The type with exactly this stored code; any other value means a row this application does not know. */
	public static PostType of(String code) {
		return Arrays.stream(values())
			.filter((type) -> type.code.equals(code))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown post type: " + code));
	}

}
