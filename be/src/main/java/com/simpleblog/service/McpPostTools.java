package com.simpleblog.service;

import com.simpleblog.component.McpArguments;
import com.simpleblog.dto.McpPostContent;
import com.simpleblog.dto.McpPostSummaries;
import com.simpleblog.dto.PostBody;
import com.simpleblog.dto.PostInput;
import com.simpleblog.dto.PostQueryType;
import com.simpleblog.entity.PostStatus;
import com.simpleblog.entity.PostType;
import com.simpleblog.exception.DuplicatePostSlugException;
import com.simpleblog.exception.InvalidPostException;
import com.simpleblog.exception.InvalidPostQueryException;
import com.simpleblog.exception.McpException;
import com.simpleblog.exception.McpToolException;
import com.simpleblog.exception.NotPostAuthorException;
import com.simpleblog.exception.PostNotFoundException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The tools of the MCP server over posts (be/mcpapispec.yaml): three that read and two that write drafts. They run as the
 * user of the current request, which the MCP access key filter has put in place, so they go through {@link PostService}
 * like the editor does and follow its rules. A tool that cannot do what was asked answers a result with {@code isError}.
 */
@Service
public class McpPostTools {

	private final PostService postService;

	private final UserService userService;

	private final ObjectMapper mapper;

	public McpPostTools(PostService postService, UserService userService, ObjectMapper mapper) {
		this.postService = postService;
		this.userService = userService;
		this.mapper = mapper;
	}

	/** The result of the tool: its data, or the reason it failed. An unknown tool or malformed arguments are a {@link McpException}. */
	public ObjectNode call(String name, JsonNode arguments) {
		McpArguments args = new McpArguments(arguments);
		try {
			return resultOf(dataOf(name, args));
		}
		catch (PostNotFoundException | InvalidPostException | InvalidPostQueryException | DuplicatePostSlugException
				| NotPostAuthorException | McpToolException ex) {
			return failureOf(ex.getMessage());
		}
	}

	private Object dataOf(String name, McpArguments args) {
		return switch (name) {
			case "list_posts" -> summaries(null, args);
			case "search_posts" -> summaries(args.requiredText("query"), args);
			case "read_post" -> McpPostContent.from(visible(args.requiredText("slug")));
			case "create_post" -> McpPostContent.from(create(args));
			case "update_post" -> McpPostContent.from(update(args));
			default -> throw McpException.invalidParams("Unknown tool: " + name);
		};
	}

	private McpPostSummaries summaries(String query, McpArguments args) {
		long viewerId = this.userService.currentViewerId();
		return McpPostSummaries.from(this.postService.listOfPosts(viewerId, PostQueryType.KEYWORD, query, args.limit()));
	}

	/** The post when the user may see it: it is published, or the user wrote it. Otherwise it is as if it did not exist. */
	private PostBody visible(String slug) {
		PostBody body = this.postService.postBody(slug);
		if (body.status() != PostStatus.PUBLISH && !body.canEdit()) {
			throw new PostNotFoundException(slug);
		}
		return body;
	}

	/** A new post: always a draft, whatever else is asked. */
	private PostBody create(McpArguments args) {
		PostInput input = new PostInput(args.requiredText("title"), args.requiredText("slug"), args.requiredText("content"), null,
				args.optionalText("hashTags"), null);
		return this.postService.createPost(input);
	}

	/** Changes the given fields of a draft post of the user; the status is left as it is. */
	private PostBody update(McpArguments args) {
		String slug = args.requiredText("slug");
		PostBody current = visible(slug);
		if (!current.canEdit()) {
			throw new McpToolException("Only the author can change the post: " + slug);
		}
		if (current.type() != PostType.POST || current.status() != PostStatus.DRAFT) {
			throw new McpToolException("Only a draft post can be changed: " + slug);
		}
		PostInput input = new PostInput(given(args.optionalText("title"), current.title()), given(args.optionalText("newSlug"), slug),
				given(args.optionalText("content"), current.content()), null, args.optionalText("hashTags"), null);
		return this.postService.updatePost(slug, input);
	}

	private String given(String value, String current) {
		return value == null ? current : value;
	}

	private ObjectNode resultOf(Object data) {
		JsonNode node = this.mapper.valueToTree(data);
		ObjectNode result = textResult(node.toString(), false);
		result.set("structuredContent", node);
		return result;
	}

	private ObjectNode failureOf(String reason) {
		return textResult(reason, true);
	}

	private ObjectNode textResult(String text, boolean isError) {
		ObjectNode result = this.mapper.createObjectNode();
		result.put("isError", isError);
		result.putArray("content").addObject().put("type", "text").put("text", text);
		return result;
	}

}
