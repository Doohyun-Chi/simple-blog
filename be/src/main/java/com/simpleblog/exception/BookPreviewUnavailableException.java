package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The store page of a book could not be fetched. Answered with 502. */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class BookPreviewUnavailableException extends RuntimeException {

	public BookPreviewUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

}
