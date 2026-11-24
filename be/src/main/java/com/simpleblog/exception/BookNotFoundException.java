package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** No book has the requested id. Answered with 404. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class BookNotFoundException extends RuntimeException {

	public BookNotFoundException(long id) {
		super("No book with id: " + id);
	}

}
