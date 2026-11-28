package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The bookmark input is unusable. Answered with 400. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidBookmarkException extends RuntimeException {

	public InvalidBookmarkException(String message) {
		super(message);
	}

}
