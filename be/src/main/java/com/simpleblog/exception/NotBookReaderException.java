package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The signed-in user is not the reader of the book they tried to change. Answered with 403. */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class NotBookReaderException extends RuntimeException {

	public NotBookReaderException(long id) {
		super("Only the reader can change the book with id: " + id);
	}

}
