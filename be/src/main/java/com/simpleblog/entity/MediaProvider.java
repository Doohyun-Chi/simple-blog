package com.simpleblog.entity;

import java.util.Arrays;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * {@code TB_MEDIA.PROVIDER}: where a media item lives. internal is an image file of the media library kept on the server,
 * external a reference such as a YouTube URL, and content an image file made for the content of one post, such as a
 * draw.io diagram, which a post save deletes once nothing shows it. The code is the stored and the JSON form.
 */
public enum MediaProvider {

	INTERNAL("internal", true, false), EXTERNAL("external", false, false), CONTENT("content", true, true);

	private final String code;

	private final boolean storesFile;

	private final boolean discardable;

	MediaProvider(String code, boolean storesFile, boolean discardable) {
		this.code = code;
		this.storesFile = storesFile;
		this.discardable = discardable;
	}

	@JsonValue
	public String code() {
		return this.code;
	}

	/** Whether the item is a file kept on the server, served at /public/api/v1/media/{slug}. */
	public boolean storesFile() {
		return this.storesFile;
	}

	/** Whether a post save may delete the item once no post or page shows it: content only, never the library's images. */
	public boolean discardable() {
		return this.discardable;
	}

	/** Whether the item may keep an editable source beside its file, such as a draw.io diagram: content only, as made in the editor. */
	public boolean keepsSource() {
		return this == CONTENT;
	}

	/** The provider with exactly this code, if there is one. */
	public static Optional<MediaProvider> find(String code) {
		return Arrays.stream(values()).filter((provider) -> provider.code.equals(code)).findFirst();
	}

	/** The provider with exactly this stored code; any other value means a row this application does not know. */
	public static MediaProvider of(String code) {
		return find(code).orElseThrow(() -> new IllegalArgumentException("Unknown media provider: " + code));
	}

}
