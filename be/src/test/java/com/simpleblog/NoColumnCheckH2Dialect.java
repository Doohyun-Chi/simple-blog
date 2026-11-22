package com.simpleblog;

import org.hibernate.dialect.H2Dialect;

/**
 * H2 for the tests, without the CHECK constraints Hibernate would add to the columns it creates, such as the allowed
 * codes of the PostType and PostStatus columns. The real schema (be/db) has none, so the test schema has none either.
 */
public class NoColumnCheckH2Dialect extends H2Dialect {

	@Override
	public boolean supportsColumnCheck() {
		return false;
	}

}
