package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** No published post has the requested slug. Answered with 404. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PostNotFoundException extends RuntimeException {

	public PostNotFoundException(String slug) {
		super("No published post with slug: " + slug);
	}

}
