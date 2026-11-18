package com.simpleblog.component;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

/** Builds the readable, unique slug of an image from its file name: English letters, digits and '-' plus a suffix. */
@Component
public class MediaSlug {

	private static final int BASE_MAX_LENGTH = 100;

	private static final int SUFFIX_LENGTH = 8;

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
