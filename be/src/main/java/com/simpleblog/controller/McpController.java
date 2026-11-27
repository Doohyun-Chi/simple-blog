package com.simpleblog.controller;

import com.simpleblog.exception.McpException;
import com.simpleblog.service.McpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The MCP endpoint of be/mcpapispec.yaml: one JSON-RPC message in, one JSON-RPC response out. The access key filter has
 * already let in only a caller with a user's key. A notification, which has no id, is accepted with 202 and no body.
 */
@RestController
@RequestMapping("/mcp/v1")
public class McpController {

	private static final Logger LOG = LoggerFactory.getLogger(McpController.class);

	private static final int INTERNAL_ERROR = -32603;

	private final McpService mcpService;

	private final ObjectMapper mapper;

	public McpController(McpService mcpService, ObjectMapper mapper) {
		this.mcpService = mcpService;
		this.mapper = mapper;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<JsonNode> message(@RequestBody byte[] body) {
		JsonNode message;
		try {
			message = this.mapper.readTree(body);
		}
		catch (JacksonException ex) {
			return ResponseEntity.badRequest().body(errorOf(null, McpException.PARSE_ERROR, "Parse error"));
		}
		if (!isMessage(message)) {
			return ResponseEntity.badRequest().body(errorOf(null, McpException.INVALID_REQUEST, "Invalid request"));
		}
		return message.has("id") ? ResponseEntity.ok(answer(message)) : ResponseEntity.status(HttpStatus.ACCEPTED).build();
	}

	/** One JSON-RPC 2.0 request or notification: an object with jsonrpc "2.0" and a method. A batch is not one. */
	private boolean isMessage(JsonNode message) {
		return message.isObject() && "2.0".equals(message.path("jsonrpc").asString(null)) && message.path("method").isString()
				&& (!message.has("id") || message.get("id").isString() || message.get("id").isIntegralNumber());
	}

	/** The response to a request: the result of its method, or the error that stopped it. */
	private JsonNode answer(JsonNode request) {
		JsonNode id = request.get("id");
		try {
			ObjectNode response = envelopeOf(id);
			response.set("result", this.mcpService.resultOf(request.get("method").asString(), request.get("params")));
			return response;
		}
		catch (McpException ex) {
			return errorOf(id, ex.code(), ex.getMessage());
		}
		catch (RuntimeException ex) {
			LOG.error("The MCP method {} failed", request.get("method").asString(), ex);
			return errorOf(id, INTERNAL_ERROR, "Internal error");
		}
	}

	private ObjectNode envelopeOf(JsonNode id) {
		ObjectNode response = this.mapper.createObjectNode();
		response.put("jsonrpc", "2.0");
		response.set("id", id == null ? this.mapper.nullNode() : id);
		return response;
	}

	private ObjectNode errorOf(JsonNode id, int code, String message) {
		ObjectNode response = envelopeOf(id);
		response.putObject("error").put("code", code).put("message", message);
		return response;
	}

}
