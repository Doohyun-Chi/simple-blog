package com.simpleblog.component;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import com.simpleblog.exception.InvalidMediaException;
import org.springframework.stereotype.Component;

/**
 * Builds the readable, unique slug of an image from its file name: English letters, digits and '-' plus a suffix. A slug
 * the writer chose is used as it is, once it follows the rule of be/apispec.yaml ({@code MediaUpload.slug}).
 */
@Component
public class MediaSlug {

	private static final int BASE_MAX_LENGTH = 100;

	private static final int SUFFIX_LENGTH = 8;

	private static final Pattern CHOSEN = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

	public String chosen(String slug) {
		if (slug.length() > BASE_MAX_LENGTH || !CHOSEN.matcher(slug).matches()) {
			throw new InvalidMediaException("slug must be lower case English letters, digits and '-', up to " + BASE_MAX_LENGTH + " characters");
		}
		return slug;
	}

	public String of(String fileName) {
		return baseOf(fileName) + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, SUFFIX_LENGTH);
	}

	private String baseOf(String fileName) {
		String name = fileName == null ? "" : fileName.substring(Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\')) + 1);
		int dot = name.lastIndexOf('.');
		String base = (dot > 0 ? name.substring(0, dot) : name).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
		base = base.replaceAll("^-+|-+$", "");
		base = base.length() > BASE_MAX_LENGTH ? base.substring(0, BASE_MAX_LENGTH).replaceAll("-+$", "") : base;
		return base.isEmpty() ? "media" : base;
	}

}
