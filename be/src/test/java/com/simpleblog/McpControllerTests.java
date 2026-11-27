package com.simpleblog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import com.simpleblog.entity.BlogUser;
import com.simpleblog.entity.Post;
import com.simpleblog.entity.PostStatus;
import com.simpleblog.repository.PostRepository;
import com.simpleblog.service.UserService;
import com.simpleblog.service.UserSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** POST /mcp/v1 of be/mcpapispec.yaml against H2: the key, the protocol and the five tools. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class McpControllerTests {

	private static final String MCP = "/mcp/v1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserService userService;

	@Autowired
	private UserSettingService settingService;

	@Autowired
	private PostRepository postRepository;

	private BlogUser owner;

	private String key;

	@BeforeEach
	void issueKey() {
		this.owner = this.userService.registration("owner@example.com", "google");
		this.key = this.settingService.renewAccessKey(this.owner.id()).mcpAccessKey();
	}

	private ResultActions rpc(String key, String body) throws Exception {
		var request = post(MCP).contentType(MediaType.APPLICATION_JSON).content(body);
		return this.mockMvc.perform(key == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + key));
	}

	private ResultActions call(String tool, String arguments) throws Exception {
		return rpc(this.key, """
				{"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "%s", "arguments": %s}}""".formatted(tool, arguments));
	}

	private void savePost(long authorId, String slug, PostStatus status) {
		Post post = Post.draftOf(authorId, "T " + slug, slug, "body of " + slug, "a,b", LocalDateTime.of(2026, 11, 1, 0, 0));
		post.moveTo(status);
		this.postRepository.save(post);
	}

	@Test
	void refusesACallWithoutACurrentKey() throws Exception {
		String ping = "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}";
		rpc(null, ping).andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"));
		rpc("not-a-key", ping).andExpect(status().isUnauthorized());
		this.mockMvc.perform(post(MCP).contentType(MediaType.APPLICATION_JSON).content(ping).header(HttpHeaders.AUTHORIZATION, "Basic abc"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void stopsWorkingWhenTheKeyIsRenewed() throws Exception {
		String ping = "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}";
		rpc(this.key, ping).andExpect(status().isOk());
		this.settingService.renewAccessKey(this.owner.id());
		rpc(this.key, ping).andExpect(status().isUnauthorized());
	}

	@Test
	void refusesAnOriginThatIsNotAllowed() throws Exception {
		this.mockMvc.perform(post(MCP).contentType(MediaType.APPLICATION_JSON).header(HttpHeaders.ORIGIN, "http://evil.example")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.key).content("{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void answersTheHandshakePingAndTheListOfTools() throws Exception {
		rpc(this.key, "{\"jsonrpc\": \"2.0\", \"id\": \"a\", \"method\": \"initialize\", \"params\": {\"protocolVersion\": \"1999-01-01\"}}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value("a"))
			.andExpect(jsonPath("$.result.protocolVersion").value("2025-06-18"))
			.andExpect(jsonPath("$.result.capabilities.tools.listChanged").value(false))
			.andExpect(jsonPath("$.result.serverInfo.name").value("simple-blog"));
		rpc(this.key, "{\"jsonrpc\": \"2.0\", \"id\": 2, \"method\": \"ping\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.result").isEmpty());
		rpc(this.key, "{\"jsonrpc\": \"2.0\", \"id\": 3, \"method\": \"tools/list\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.result.tools.length()").value(5))
			.andExpect(jsonPath("$.result.tools[0].name").value("list_posts"))
			.andExpect(jsonPath("$.result.tools[4].name").value("update_post"));
	}

	@Test
	void acceptsANotificationWithoutAnAnswer() throws Exception {
		rpc(this.key, "{\"jsonrpc\": \"2.0\", \"method\": \"notifications/initialized\"}").andExpect(status().isAccepted());
	}

	@Test
	void answersJsonRpcErrorsForWhatItCannotRead() throws Exception {
		rpc(this.key, "{\"jsonrpc\": \"2.0\", \"id\": 5, \"method\": \"nope\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.error.code").value(-32601));
		rpc(this.key, "[{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"ping\"}]").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value(-32600));
		rpc(this.key, "{\"jsonrpc\": \"1.0\", \"id\": 1, \"method\": \"ping\"}").andExpect(status().isBadRequest());
		rpc(this.key, "{oops").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value(-32700))
			.andExpect(jsonPath("$.id").isEmpty());
	}

	@Test
	void refusesAGetAndABodyThatIsNotJson() throws Exception {
		this.mockMvc.perform(get(MCP).header(HttpHeaders.AUTHORIZATION, "Bearer " + this.key)).andExpect(status().isMethodNotAllowed());
		this.mockMvc.perform(post(MCP).contentType(MediaType.TEXT_PLAIN).content("x").header(HttpHeaders.AUTHORIZATION, "Bearer " + this.key))
			.andExpect(status().isUnsupportedMediaType());
	}

	@Test
	void writesADraftAndReadsListsAndSearchesIt() throws Exception {
		call("create_post", "{\"title\": \"From MCP\", \"slug\": \"from-mcp\", \"content\": \"Hello **MCP**\", \"hashTags\": \"mcp\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.isError").value(false))
			.andExpect(jsonPath("$.result.structuredContent.slug").value("from-mcp"))
			.andExpect(jsonPath("$.result.structuredContent.status").value("draft"));
		call("read_post", "{\"slug\": \"from-mcp\"}")
			.andExpect(jsonPath("$.result.structuredContent.content").value("Hello **MCP**"))
			.andExpect(jsonPath("$.result.structuredContent.hashTags").value("mcp"))
			.andExpect(jsonPath("$.result.content[0].type").value("text"));
		call("list_posts", "{}").andExpect(jsonPath("$.result.structuredContent.posts[0].slug").value("from-mcp"));
		call("search_posts", "{\"query\": \"FROM mcp\"}").andExpect(jsonPath("$.result.structuredContent.posts.length()").value(1));
		call("search_posts", "{\"query\": \"nothing like it\"}").andExpect(jsonPath("$.result.structuredContent.posts.length()").value(0));
	}

	@Test
	void reportsAFailureOfAWriteAsAnErrorResult() throws Exception {
		String post = "{\"title\": \"T\", \"slug\": \"same\", \"content\": \"c\"}";
		call("create_post", post).andExpect(jsonPath("$.result.isError").value(false));
		call("create_post", post).andExpect(status().isOk()).andExpect(jsonPath("$.result.isError").value(true))
			.andExpect(jsonPath("$.result.content[0].text").value(org.hamcrest.Matchers.containsString("same")));
		call("create_post", "{\"title\": \"T\", \"slug\": \"bad slug\", \"content\": \"c\"}").andExpect(jsonPath("$.result.isError").value(true));
		call("read_post", "{\"slug\": \"missing\"}").andExpect(jsonPath("$.result.isError").value(true));
	}

	@Test
	void changesOnlyTheGivenFieldsOfADraft() throws Exception {
		call("create_post", "{\"title\": \"Old\", \"slug\": \"changing\", \"content\": \"keep me\", \"hashTags\": \"x\"}");
		call("update_post", "{\"slug\": \"changing\", \"title\": \"New\", \"newSlug\": \"changed\"}")
			.andExpect(jsonPath("$.result.isError").value(false))
			.andExpect(jsonPath("$.result.structuredContent.title").value("New"))
			.andExpect(jsonPath("$.result.structuredContent.slug").value("changed"))
			.andExpect(jsonPath("$.result.structuredContent.content").value("keep me"))
			.andExpect(jsonPath("$.result.structuredContent.hashTags").value("x"))
			.andExpect(jsonPath("$.result.structuredContent.status").value("draft"));
		call("update_post", "{\"slug\": \"changed\", \"hashTags\": \"\"}").andExpect(jsonPath("$.result.structuredContent.hashTags").value(""));
	}

	@Test
	void changesNoPostThatIsPublishedOrAnotherUsers() throws Exception {
		savePost(this.owner.id(), "mine-published", PostStatus.PUBLISH);
		savePost(102, "theirs-published", PostStatus.PUBLISH);
		savePost(102, "theirs-draft", PostStatus.DRAFT);
		call("update_post", "{\"slug\": \"mine-published\", \"title\": \"x\"}").andExpect(jsonPath("$.result.isError").value(true))
			.andExpect(jsonPath("$.result.content[0].text").value(org.hamcrest.Matchers.containsString("draft")));
		call("update_post", "{\"slug\": \"theirs-published\", \"title\": \"x\"}").andExpect(jsonPath("$.result.isError").value(true));
		call("update_post", "{\"slug\": \"theirs-draft\", \"title\": \"x\"}").andExpect(jsonPath("$.result.isError").value(true));
		call("read_post", "{\"slug\": \"mine-published\"}").andExpect(jsonPath("$.result.structuredContent.title").value("T mine-published"));
	}

	@Test
	void showsNoDraftOfAnotherUser() throws Exception {
		savePost(102, "theirs-draft", PostStatus.DRAFT);
		savePost(102, "theirs-published", PostStatus.PUBLISH);
		call("read_post", "{\"slug\": \"theirs-draft\"}").andExpect(jsonPath("$.result.isError").value(true));
		call("read_post", "{\"slug\": \"theirs-published\"}").andExpect(jsonPath("$.result.isError").value(false));
		call("list_posts", "{}").andExpect(jsonPath("$.result.structuredContent.posts.length()").value(1))
			.andExpect(jsonPath("$.result.structuredContent.posts[0].slug").value("theirs-published"));
	}

	@Test
	void refusesCallsThatAreNotShapedRight() throws Exception {
		call("no_such_tool", "{}").andExpect(jsonPath("$.error.code").value(-32602));
		call("read_post", "{}").andExpect(jsonPath("$.error.code").value(-32602));
		call("read_post", "{\"slug\": 5}").andExpect(jsonPath("$.error.code").value(-32602));
		call("list_posts", "{\"limit\": 0}").andExpect(jsonPath("$.error.code").value(-32602));
		call("list_posts", "{\"limit\": 51}").andExpect(jsonPath("$.error.code").value(-32602));
		call("list_posts", "{\"limit\": \"5\"}").andExpect(jsonPath("$.error.code").value(-32602));
		call("list_posts", "[]").andExpect(jsonPath("$.error.code").value(-32602));
		rpc(this.key, "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"tools/call\", \"params\": {}}")
			.andExpect(jsonPath("$.error.code").value(-32602));
	}

}
