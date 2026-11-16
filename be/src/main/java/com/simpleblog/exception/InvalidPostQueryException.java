package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The post search parameters break the spec: a blank query or an unknown queryType. Answered with 400. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPostQueryException extends RuntimeException {

	public InvalidPostQueryException(String message) {
		super(message);
	}

}
