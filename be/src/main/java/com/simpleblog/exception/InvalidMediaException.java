package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The upload or link is unusable. Answered with 400. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidMediaException extends RuntimeException {

	public InvalidMediaException(String message) {
		super(message);
	}

}
