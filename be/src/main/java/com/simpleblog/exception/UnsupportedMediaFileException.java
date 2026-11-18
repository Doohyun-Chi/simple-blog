package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The file is not an accepted image. Answered with 415. */
@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
public class UnsupportedMediaFileException extends RuntimeException {

	public UnsupportedMediaFileException(String message) {
		super(message);
	}

}
