package com.simpleblog.component;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

/** Where readers open a post or a page: on the frontend, which is the same origin as this server when none is configured. */
@Component
public class ViewLinks {

	private final String frontendUrl;

	public ViewLinks(@Value("${simpleblog.auth.frontend-url:}") String frontendUrl) {
		this.frontendUrl = frontendUrl.strip().replaceAll("/+$", "");
	}

	/** The frontend's origin; {@code backendOrigin} stands in when none is configured. */
	public String frontendOrigin(String backendOrigin) {
		return this.frontendUrl.isEmpty() ? backendOrigin : this.frontendUrl;
	}

	/** Pages are at /page/{slug}, posts at /post/{slug}: the slug as readers see it (decoded once), then URL-encoded. */
	public String pathOf(String type, String storedSlug) {
		return ("page".equals(type) ? "/page/" : "/post/") + UriUtils.encodePathSegment(readerSlug(storedSlug), StandardCharsets.UTF_8);
	}

	private static String readerSlug(String storedSlug) {
		try {
			return UriUtils.decode(storedSlug, StandardCharsets.UTF_8);
		}
		catch (IllegalArgumentException ex) {
			return storedSlug;
		}
	}

}
