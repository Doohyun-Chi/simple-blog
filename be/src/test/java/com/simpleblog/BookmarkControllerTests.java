package com.simpleblog;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * The bookmark endpoints against H2. User 101 is the one signed in; posts 1 and 4 are published, 2 is a draft of 101 and
 * 3 a draft of user 202.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql(statements = """
		insert into tb_post (ID, TITLE, CONTENT, SLUG, TYPE, STATUS, AUTHOR_ID, CREATED_DATETIME, MODIFIED_DATETIME) values
		(1, 'First post', 'one', 'first-post', 'post', 'publish', 101, '2026-11-01 00:00:00', '2026-11-01 00:00:00'),
		(2, 'My draft', 'two', 'my-draft', 'post', 'draft', 101, '2026-11-02 00:00:00', '2026-11-02 00:00:00'),
		(3, 'Their draft', 'three', 'their-draft', 'post', 'draft', 202, '2026-11-03 00:00:00', '2026-11-03 00:00:00'),
		(4, 'Fourth post', 'four', 'fourth-post', 'post', 'publish', 202, '2026-11-04 00:00:00', '2026-11-04 00:00:00');
		""")
class BookmarkControllerTests {

	private static final String BOOKMARKS = "/public/api/v1/bookmarks";

	private static final String BOOKMARK = "/public/api/v1/bookmark";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static OidcLoginRequestPostProcessor me() {
		return TestUsers.signedInWithId("me@example.com", 101);
	}

	private static OidcLoginRequestPostProcessor other() {
		return TestUsers.signedInWithId("other@example.com", 202);
	}

	private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
		return request.with(me()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	/** Bookmarks the post as the signed-in user and answers the id. */
	private long bookmark(String slug) throws Exception {
		String body = this.mockMvc.perform(json(post(BOOKMARK), "{\"slug\": \"" + slug + "\"}"))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	@Test
	void needsASignedInUser() throws Exception {
		this.mockMvc.perform(get(BOOKMARKS)).andExpect(status().isUnauthorized());
		this.mockMvc.perform(post(BOOKMARK).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"slug\": \"first-post\"}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void needsACsrfTokenToWrite() throws Exception {
		this.mockMvc.perform(post(BOOKMARK).with(me()).contentType(MediaType.APPLICATION_JSON).content("{\"slug\": \"first-post\"}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void bookmarksAPostUnderItsTitleWithoutPinningIt() throws Exception {
		this.mockMvc.perform(json(post(BOOKMARK), "{\"slug\": \"first-post\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.postId").value(1))
			.andExpect(jsonPath("$.slug").value("first-post"))
			.andExpect(jsonPath("$.name").value("First post"))
			.andExpect(jsonPath("$.order").value(0))
			.andExpect(jsonPath("$.createdDate").isNotEmpty())
			.andExpect(jsonPath("$.modifiedDate").value(nullValue()));
	}

	@Test
	void bookmarksOnlyAPostTheUserCanRead() throws Exception {
		this.mockMvc.perform(json(post(BOOKMARK), "{\"slug\": \"my-draft\"}")).andExpect(status().isCreated());
		this.mockMvc.perform(json(post(BOOKMARK), "{\"slug\": \"their-draft\"}")).andExpect(status().isNotFound());
		this.mockMvc.perform(json(post(BOOKMARK), "{\"slug\": \"nothing\"}")).andExpect(status().isNotFound());
	}

	@Test
	void bookmarksAPostOnlyOncePerUser() throws Exception {
		bookmark("first-post");
		this.mockMvc.perform(json(post(BOOKMARK), "{\"slug\": \"first-post\"}")).andExpect(status().isConflict());
		this.mockMvc.perform(post(BOOKMARK).with(other()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"slug\": \"first-post\"}"))
			.andExpect(status().isCreated());
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"slug\": \"\"}", "{\"slug\": \"  \"}" })
	void refusesABookmarkWithoutASlug(String body) throws Exception {
		this.mockMvc.perform(json(post(BOOKMARK), body)).andExpect(status().isBadRequest());
	}

	@Test
	void listsTheUsersOwnBookmarksPinnedFirstThenTheNewest() throws Exception {
		long first = bookmark("first-post");
		long second = bookmark("my-draft");
		long third = bookmark("fourth-post");
		this.mockMvc.perform(json(put(BOOKMARKS),
				"{\"bookmarks\": [{\"id\": " + first + ", \"name\": \"A\", \"order\": 3}, {\"id\": " + second + ", \"name\": \"B\", \"order\": 5}]}"))
			.andExpect(status().isOk());
		this.mockMvc.perform(get(BOOKMARKS).with(me()))
			.andExpect(jsonPath("$.bookmarks.length()").value(3))
			.andExpect(jsonPath("$.bookmarks[0].id").value(second))
			.andExpect(jsonPath("$.bookmarks[1].id").value(first))
			.andExpect(jsonPath("$.bookmarks[2].id").value(third));
		this.mockMvc.perform(get(BOOKMARKS).with(other())).andExpect(jsonPath("$.bookmarks.length()").value(0));
	}

	@Test
	void limitsTheList() throws Exception {
		bookmark("first-post");
		long newest = bookmark("fourth-post");
		this.mockMvc.perform(get(BOOKMARKS).with(me()).param("limit", "1"))
			.andExpect(jsonPath("$.bookmarks.length()").value(1))
			.andExpect(jsonPath("$.bookmarks[0].id").value(newest));
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "101", "abc" })
	void rejectsALimitOutsideTheSpec(String limit) throws Exception {
		this.mockMvc.perform(get(BOOKMARKS).with(me()).param("limit", limit)).andExpect(status().isBadRequest());
	}

	@Test
	void renamesABookmarkAndRecordsTheModification() throws Exception {
		long id = bookmark("first-post");
		this.mockMvc.perform(json(put(BOOKMARK + "/" + id), "{\"name\": \"  Read later \"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Read later"))
			.andExpect(jsonPath("$.modifiedDate").isNotEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"name\": \" \"}" })
	void refusesABlankName(String body) throws Exception {
		long id = bookmark("first-post");
		this.mockMvc.perform(json(put(BOOKMARK + "/" + id), body)).andExpect(status().isBadRequest());
	}

	@Test
	void refusesATooLongName() throws Exception {
		long id = bookmark("first-post");
		this.mockMvc.perform(json(put(BOOKMARK + "/" + id), "{\"name\": \"" + "a".repeat(131) + "\"}")).andExpect(status().isBadRequest());
		this.mockMvc.perform(json(put(BOOKMARK + "/" + id), "{\"name\": \"" + "a".repeat(130) + "\"}")).andExpect(status().isOk());
	}

	@Test
	void keepsABookmarkFromAnyoneButItsOwner() throws Exception {
		long id = bookmark("first-post");
		this.mockMvc.perform(put(BOOKMARK + "/" + id).with(other()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"x\"}"))
			.andExpect(status().isNotFound());
		this.mockMvc.perform(delete(BOOKMARK + "/" + id).with(other()).with(csrf())).andExpect(status().isNotFound());
		this.mockMvc.perform(delete(BOOKMARK + "/" + (id + 100)).with(me()).with(csrf())).andExpect(status().isNotFound());
	}

	@Test
	void deletesABookmarkButNotItsPost() throws Exception {
		long id = bookmark("first-post");
		this.mockMvc.perform(delete(BOOKMARK + "/" + id).with(me()).with(csrf())).andExpect(status().isNoContent());
		this.mockMvc.perform(get(BOOKMARKS).with(me())).andExpect(jsonPath("$.bookmarks.length()").value(0));
		this.mockMvc.perform(get("/public/api/v1/post/first-post")).andExpect(status().isOk());
	}

	@Test
	void savesNamesAndOrdersTogetherWithoutTouchingTheModification() throws Exception {
		long first = bookmark("first-post");
		long second = bookmark("fourth-post");
		this.mockMvc.perform(json(put(BOOKMARKS),
				"{\"bookmarks\": [{\"id\": " + first + ", \"name\": \" Pinned \", \"order\": 7}, {\"id\": " + second + ", \"name\": \"Plain\", \"order\": 0}]}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.bookmarks[0].id").value(first))
			.andExpect(jsonPath("$.bookmarks[0].name").value("Pinned"))
			.andExpect(jsonPath("$.bookmarks[0].order").value(7))
			.andExpect(jsonPath("$.bookmarks[0].modifiedDate").value(nullValue()))
			.andExpect(jsonPath("$.bookmarks[1].id").value(second));
	}

	@Test
	void savesNothingWhenOneOfTheBookmarksIsNotTheUsers() throws Exception {
		long mine = bookmark("first-post");
		long theirs = ((Number) JsonPath.read(this.mockMvc.perform(post(BOOKMARK).with(other()).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content("{\"slug\": \"first-post\"}")).andReturn().getResponse().getContentAsString(), "$.id")).longValue();
		this.mockMvc.perform(json(put(BOOKMARKS),
				"{\"bookmarks\": [{\"id\": " + mine + ", \"name\": \"Changed\", \"order\": 9}, {\"id\": " + theirs + ", \"name\": \"X\", \"order\": 1}]}"))
			.andExpect(status().isNotFound());
		this.mockMvc.perform(get(BOOKMARKS).with(me())).andExpect(jsonPath("$.bookmarks[0].name").value("First post"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"bookmarks\": []}", "{\"bookmarks\": [{\"name\": \"a\", \"order\": 1}]}",
			"{\"bookmarks\": [{\"id\": 1, \"name\": \"a\", \"order\": -1}]}", "{\"bookmarks\": [{\"id\": 1, \"name\": \"a\"}]}",
			"{\"bookmarks\": [{\"id\": 1, \"name\": \" \", \"order\": 1}]}",
			"{\"bookmarks\": [{\"id\": 1, \"name\": \"a\", \"order\": 1}, {\"id\": 1, \"name\": \"b\", \"order\": 2}]}" })
	void refusesAnArrangementOutsideTheSpec(String body) throws Exception {
		this.mockMvc.perform(json(put(BOOKMARKS), body)).andExpect(status().isBadRequest());
	}

	@Test
	void tellsTheRequesterTheirBookmarkOfAPost() throws Exception {
		long id = bookmark("first-post");
		this.mockMvc.perform(get("/public/api/v1/post/first-post").with(me())).andExpect(jsonPath("$.bookmarkId").value(id));
		this.mockMvc.perform(get("/public/api/v1/post/first-post").with(other())).andExpect(jsonPath("$.bookmarkId").value(nullValue()));
		this.mockMvc.perform(get("/public/api/v1/post/first-post")).andExpect(jsonPath("$.bookmarkId").value(nullValue()));
	}

	@Test
	void showsTheNickNameKeptWithAPostAtItsLastSave() throws Exception {
		this.jdbcTemplate.update("""
				insert into tb_user_setting (USER_ID, NICK, MAIN_THEME, SUB_THEME, ABOUT_NAME, ABOUT_SLUG, WORKING_NAME, WORKING_SLUG, MODIFIED_DATE)
				values (101, 'Chi', 'm', 's', 'a', 'a', 'w', 'w', now())""");
		this.mockMvc.perform(json(put("/public/api/v1/post/first-post"), "{\"title\": \"T\", \"slug\": \"first-post\", \"content\": \"c\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.author").value("Chi"));
		this.mockMvc.perform(get("/public/api/v1/post/first-post")).andExpect(jsonPath("$.author").value("Chi"));
		this.mockMvc.perform(get("/public/api/v1/post/fourth-post")).andExpect(jsonPath("$.author").value(nullValue()));
	}

	@Test
	void followsAPostThatIsRenamedAndGoesWithOneThatIsDeleted() throws Exception {
		bookmark("first-post");
		this.mockMvc.perform(json(put("/public/api/v1/post/first-post"), "{\"title\": \"T\", \"slug\": \"renamed\", \"content\": \"c\"}"))
			.andExpect(status().isOk());
		// Read by SQL: this test is one transaction, whose persistence context still holds the bookmark as it was loaded.
		assertThat(this.jdbcTemplate.queryForObject("select post_slug from tb_bookmark", String.class)).isEqualTo("renamed");
		this.mockMvc.perform(delete("/public/api/v1/post/renamed").with(me()).with(csrf())).andExpect(status().isNoContent());
		assertThat(this.jdbcTemplate.queryForObject("select count(*) from tb_bookmark", Integer.class)).isZero();
	}

}
