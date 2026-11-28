package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The user has no bookmark with this id. Answered with 404. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class BookmarkNotFoundException extends RuntimeException {

	public BookmarkNotFoundException(String message) {
		super(message);
	}

}
