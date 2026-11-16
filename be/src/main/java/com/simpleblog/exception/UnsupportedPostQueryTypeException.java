package com.simpleblog.exception;

import com.simpleblog.dto.PostQueryType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The query type is in the spec but its matching rule is not defined yet. Answered with 501. */
@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
public class UnsupportedPostQueryTypeException extends RuntimeException {

	public UnsupportedPostQueryTypeException(PostQueryType queryType) {
		super("queryType is not implemented yet: " + queryType);
	}

}
