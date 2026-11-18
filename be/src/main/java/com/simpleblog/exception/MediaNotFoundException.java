package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** No internal media item has the requested slug. Answered with 404. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class MediaNotFoundException extends RuntimeException {

	public MediaNotFoundException(String message) {
		super(message);
	}

}
