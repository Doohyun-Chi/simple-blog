package com.simpleblog.entity;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * {@code TB_MEDIA.PROVIDER}: where a media item lives. internal is an image file kept on the server, external a reference
 * such as a YouTube URL. The code is the stored and the JSON form.
 */
public enum MediaProvider {

	INTERNAL("internal", true), EXTERNAL("external", false);

	private final String code;

	private final boolean storesFile;

	MediaProvider(String code, boolean storesFile) {
		this.code = code;
		this.storesFile = storesFile;
	}

	@JsonValue
	public String code() {
		return this.code;
	}

	/** Whether the item is a file kept on the server, served at /public/api/v1/media/{slug}. */
	public boolean storesFile() {
		return this.storesFile;
	}

	/** The provider with exactly this stored code; any other value means a row this application does not know. */
	public static MediaProvider of(String code) {
		return Arrays.stream(values())
			.filter((provider) -> provider.code.equals(code))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown media provider: " + code));
	}

}
