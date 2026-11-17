package com.simpleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Another row of wp_posts already has the slug. Answered with 409. */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicatePostSlugException extends RuntimeException {

	public DuplicatePostSlugException(String slug) {
		super("Another post already has the slug: " + slug);
	}

}
