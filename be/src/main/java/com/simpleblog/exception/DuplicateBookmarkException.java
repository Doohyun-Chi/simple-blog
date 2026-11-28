package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The user has bookmarked the post already. Answered with 409. */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateBookmarkException extends RuntimeException {

	public DuplicateBookmarkException(String message) {
		super(message);
	}

}
