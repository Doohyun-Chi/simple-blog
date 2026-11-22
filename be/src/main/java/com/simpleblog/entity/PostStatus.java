package com.simpleblog.entity;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * {@code TB_POST.STATUS}: every row is a draft, published or private, no other status is stored. The code is the stored and the JSON form.
 */
public enum PostStatus {

	DRAFT("draft", true), PUBLISH("publish", true), PRIVATE("private", false);

	private final String code;

	private final boolean settable;

	PostStatus(String code, boolean settable) {
		this.code = code;
		this.settable = settable;
	}

	@JsonValue
	public String code() {
		return this.code;
	}

	/** Whether a client may set it when creating or updating a post: draft and publish only. */
	public boolean settable() {
		return this.settable;
	}

	/** The status with exactly this stored code; any other value means a row this application does not know. */
	public static PostStatus of(String code) {
		return Arrays.stream(values())
			.filter((status) -> status.code.equals(code))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown post status: " + code));
	}

}
