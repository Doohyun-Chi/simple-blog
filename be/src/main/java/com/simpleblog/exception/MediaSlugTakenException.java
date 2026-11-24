package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** An image of the writer already has the slug, and the upload did not ask to replace it. Answered with 409. */
@ResponseStatus(HttpStatus.CONFLICT)
public class MediaSlugTakenException extends RuntimeException {

	public MediaSlugTakenException(String slug) {
		super("An image already has the slug: " + slug);
	}

}
