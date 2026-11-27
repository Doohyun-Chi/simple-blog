package com.simpleblog.exception;

/** A tool of the MCP server could not do what was asked; the text says why and is given to the client as the tool's result. */
public class McpToolException extends RuntimeException {

	public McpToolException(String message) {
		super(message);
	}

}
