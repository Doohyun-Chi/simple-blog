package com.simpleblog.component;

import com.simpleblog.exception.McpException;
import tools.jackson.databind.JsonNode;

/**
 * The arguments of a tool call. A value of the wrong type, a missing required one or a number outside its range is a
 * JSON-RPC invalid params error, as be/mcpapispec.yaml says; what the value then means is for the service to judge.
 */
public class McpArguments {

	public static final int DEFAULT_LIMIT = 10;

	public static final int MAX_LIMIT = 50;

	private final JsonNode arguments;

	public McpArguments(JsonNode arguments) {
		if (arguments != null && !arguments.isMissingNode() && !arguments.isNull() && !arguments.isObject()) {
			throw McpException.invalidParams("arguments must be an object");
		}
		this.arguments = arguments;
	}

	public String requiredText(String name) {
		String text = optionalText(name);
		if (text == null) {
			throw McpException.invalidParams(name + " is required");
		}
		return text;
	}

	/** The text of the argument, or null when it is left out. */
	public String optionalText(String name) {
		JsonNode value = valueOf(name);
		if (value == null) {
			return null;
		}
		if (!value.isString()) {
			throw McpException.invalidParams(name + " must be a string");
		}
		return value.asString();
	}

	/** The limit of a list: the argument from 1 to {@link #MAX_LIMIT}, or {@link #DEFAULT_LIMIT} when it is left out. */
	public int limit() {
		JsonNode value = valueOf("limit");
		if (value == null) {
			return DEFAULT_LIMIT;
		}
		if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 1 || value.intValue() > MAX_LIMIT) {
			throw McpException.invalidParams("limit must be an integer from 1 to " + MAX_LIMIT);
		}
		return value.intValue();
	}

	private JsonNode valueOf(String name) {
		if (this.arguments == null) {
			return null;
		}
		JsonNode value = this.arguments.get(name);
		return value == null || value.isNull() ? null : value;
	}

}
