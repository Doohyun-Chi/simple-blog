package com.simpleblog.exception;

/** A message the MCP server cannot answer with a result: the JSON-RPC error code and text it answers instead. */
public class McpException extends RuntimeException {

	public static final int PARSE_ERROR = -32700;

	public static final int INVALID_REQUEST = -32600;

	public static final int METHOD_NOT_FOUND = -32601;

	public static final int INVALID_PARAMS = -32602;

	private final int code;

	public McpException(int code, String message) {
		super(message);
		this.code = code;
	}

	public static McpException invalidParams(String message) {
		return new McpException(INVALID_PARAMS, message);
	}

	public int code() {
		return this.code;
	}

}
