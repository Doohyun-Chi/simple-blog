package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** A field of a book to write, or the year of the shelf, is missing or cannot be stored. Answered with 400. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidBookException extends RuntimeException {

	public InvalidBookException(String message) {
		super(message);
	}

}
