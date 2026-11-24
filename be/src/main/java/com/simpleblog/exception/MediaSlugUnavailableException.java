package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The slug is held by an item the writer cannot replace: someone else's image or a video. Answered with 422. */
@ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
public class MediaSlugUnavailableException extends RuntimeException {

	public MediaSlugUnavailableException(String slug) {
		super("The slug belongs to an item that cannot be replaced: " + slug);
	}

}
