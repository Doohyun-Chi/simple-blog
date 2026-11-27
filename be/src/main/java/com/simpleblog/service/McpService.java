package com.simpleblog.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import com.simpleblog.exception.McpException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** The methods of the MCP server (be/mcpapispec.yaml): the handshake, ping, the list of tools and the call of one. */
@Service
public class McpService {

	/** The protocol version the server speaks; it answers it whatever version the client asked for. */
	static final String PROTOCOL_VERSION = "2025-06-18";

	private static final String SERVER_NAME = "simple-blog";

	private static final String SERVER_VERSION = "0.1.0";

	private final McpPostTools postTools;

	private final ObjectMapper mapper;

	private final JsonNode tools;

	public McpService(McpPostTools postTools, ObjectMapper mapper) {
		this.postTools = postTools;
		this.mapper = mapper;
		this.tools = toolsOf(mapper);
	}

	/** The result of a request, or a {@link McpException} when the method is unknown or the parameters are wrong. */
	public JsonNode resultOf(String method, JsonNode params) {
		return switch (method) {
			case "initialize" -> initialization();
			case "ping" -> this.mapper.createObjectNode();
			case "tools/list" -> this.mapper.createObjectNode().set("tools", this.tools);
			case "tools/call" -> toolCall(params);
			default -> throw new McpException(McpException.METHOD_NOT_FOUND, "Method not found");
		};
	}

	private JsonNode toolCall(JsonNode params) {
		JsonNode name = params == null ? null : params.get("name");
		if (name == null || !name.isString()) {
			throw McpException.invalidParams("params.name must be the name of a tool");
		}
		return this.postTools.call(name.asString(), params.get("arguments"));
	}

	private ObjectNode initialization() {
		ObjectNode result = this.mapper.createObjectNode();
		result.put("protocolVersion", PROTOCOL_VERSION);
		result.putObject("capabilities").putObject("tools").put("listChanged", false);
		result.putObject("serverInfo").put("name", SERVER_NAME).put("version", SERVER_VERSION);
		return result;
	}

	private static JsonNode toolsOf(ObjectMapper mapper) {
		try (InputStream in = new ClassPathResource("mcp/tools.json").getInputStream()) {
			return mapper.readTree(in);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read the MCP tool definitions", ex);
		}
	}

}
