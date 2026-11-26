package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The signed-in user did not register the media item they tried to delete. Answered with 403. */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class NotMediaOwnerException extends RuntimeException {

	public NotMediaOwnerException(String slug) {
		super("Only the owner can delete the media with slug: " + slug);
	}

}
