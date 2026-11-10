package com.simpleblog.component;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

/**
 * Converts a decoded slug to the form WordPress stores in {@code wp_posts.post_name}. WordPress percent-encodes every
 * non-ASCII UTF-8 byte with lowercase hex ({@code utf8_uri_encode} then {@code strtolower}) and keeps ASCII as is, so
 * {@code 큰} is stored as {@code %ed%81%b0}.
 */
@Component
public class SlugEncoder {

	private static final char[] HEX = "0123456789abcdef".toCharArray();

	public String encode(String slug) {
		StringBuilder encoded = new StringBuilder(slug.length());
		for (byte b : slug.getBytes(StandardCharsets.UTF_8)) {
			if (b >= 0) {
				encoded.append((char) b);
			}
			else {
				encoded.append('%').append(HEX[(b >> 4) & 0xF]).append(HEX[b & 0xF]);
			}
		}
		return encoded.toString();
	}

}
