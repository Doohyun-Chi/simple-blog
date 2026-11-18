package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The signed-in user is not the author of the post they tried to delete. Answered with 403. */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class NotPostAuthorException extends RuntimeException {

	public NotPostAuthorException(String slug) {
		super("Only the author can delete the post with slug: " + slug);
	}

}
