package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** A field of the personalization to save is missing, too long or has a character it may not. Answered with 400. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidUserSettingException extends RuntimeException {

	public InvalidUserSettingException(String message) {
		super(message);
	}

}
