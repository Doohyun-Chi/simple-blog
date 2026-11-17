package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** A required field of a post to write is missing or blank, or a value cannot be stored. Answered with 400. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPostException extends RuntimeException {

	public InvalidPostException(String message) {
		super(message);
	}

}
