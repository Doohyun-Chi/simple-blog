package com.simpleblog.entity;

import java.util.Arrays;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * {@code TB_MEDIA.SOURCE_TYPE}: the kind of the editable source kept beside an image file, such as the draw.io diagram a
 * PNG was exported from. The source is a file named like the image with {@link #extension()}, served as {@link #mimeType()}.
 * The code is the stored and the JSON form.
 */
public enum SourceType {

	DRAWIO("drawio", "drawio", "application/xml");

	private final String code;

	private final String extension;

	private final String mimeType;

	SourceType(String code, String extension, String mimeType) {
		this.code = code;
		this.extension = extension;
		this.mimeType = mimeType;
	}

	@JsonValue
	public String code() {
		return this.code;
	}

	/** The extension of the source file, which otherwise has the name of the image file. */
	public String extension() {
		return this.extension;
	}

	/** The mime type the source is served with, in UTF-8. */
	public String mimeType() {
		return this.mimeType;
	}

	/** The type with exactly this code, if there is one. */
	public static Optional<SourceType> find(String code) {
		return Arrays.stream(values()).filter((type) -> type.code.equals(code)).findFirst();
	}

	/** The type with exactly this stored code; any other value means a row this application does not know. */
	public static SourceType of(String code) {
		return find(code).orElseThrow(() -> new IllegalArgumentException("Unknown source type: " + code));
	}

}
