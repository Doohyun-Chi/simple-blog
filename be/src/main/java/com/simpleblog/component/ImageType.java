package com.simpleblog.component;

import java.util.Arrays;
import java.util.Optional;

/** The image formats accepted for upload, recognised by their first bytes: whatever the client claims is ignored. */
public enum ImageType {

	PNG("image/png", "png", new int[] { 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A }),
	JPEG("image/jpeg", "jpg", new int[] { 0xFF, 0xD8, 0xFF }),
	GIF("image/gif", "gif", new int[] { 'G', 'I', 'F', '8' }),
	WEBP("image/webp", "webp", new int[] { 'R', 'I', 'F', 'F' });

	private final String mimeType;

	private final String extension;

	private final int[] signature;

	ImageType(String mimeType, String extension, int[] signature) {
		this.mimeType = mimeType;
		this.extension = extension;
		this.signature = signature;
	}

	public static Optional<ImageType> of(byte[] content) {
		return Arrays.stream(values()).filter((type) -> type.matches(content)).findFirst();
	}

	public String mimeType() {
		return this.mimeType;
	}

	public String extension() {
		return this.extension;
	}

	private boolean matches(byte[] content) {
		if (content.length < this.signature.length || (this == WEBP && !isWebp(content))) {
			return false;
		}
		for (int i = 0; i < this.signature.length; i++) {
			if ((content[i] & 0xFF) != this.signature[i]) {
				return false;
			}
		}
		return true;
	}

	/** RIFF is shared with other formats: a WebP has WEBP at bytes 8 to 11. */
	private static boolean isWebp(byte[] content) {
		return content.length >= 12 && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P';
	}

}
