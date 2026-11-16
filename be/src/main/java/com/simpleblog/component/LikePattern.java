package com.simpleblog.component;

import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * Builds a JPQL LIKE pattern that matches the text anywhere, lowercased for a case-insensitive match. The LIKE
 * wildcards and the escape character itself are escaped with {@link #ESCAPE}, so user input is matched literally.
 */
@Component
public class LikePattern {

	public static final char ESCAPE = '\\';

	public String containing(String text) {
		StringBuilder pattern = new StringBuilder(text.length() + 2).append('%');
		for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) {
			if (c == '%' || c == '_' || c == ESCAPE) {
				pattern.append(ESCAPE);
			}
			pattern.append(c);
		}
		return pattern.append('%').toString();
	}

}
