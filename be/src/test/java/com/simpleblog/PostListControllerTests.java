package com.simpleblog;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * GET /public/api/v1/posts against H2. Rows 1-3 are public posts; rows 4-8 mention KPI too but must never be listed:
 * a draft, a private post, a page and an attachment.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql(statements = """
		insert into tb_post (ID, TITLE, CONTENT, SLUG, TYPE, STATUS, AUTHOR_ID, CREATED_DATETIME, MODIFIED_DATETIME) values
		(1, 'Old KPI', 'nothing here', 'old-kpi', 'post', 'publish', 1, '2019-12-31 15:00:00', '2019-12-31 15:00:00'),
		(2, 'New one', 'about kpi and 100% done', 'new-one', 'post', 'publish', 1, '2026-11-05 00:45:11', '2026-11-05 00:45:11'),
		(3, 'Unrelated', 'about 100 percent', 'unrelated', 'post', 'publish', 1, '2023-12-31 15:00:00', '2023-12-31 15:00:00'),
		(4, 'KPI draft', 'kpi', 'kpi-draft', 'post', 'draft', 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
		(5, 'KPI private', 'kpi', 'kpi-private', 'post', 'private', 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
		(7, 'KPI page', 'kpi', 'kpi-page', 'page', 'publish', 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
		(8, 'KPI image', 'kpi', 'kpi-image', 'attachment', 'inherit', 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00');
		""")
class PostListControllerTests {

	private static final String POSTS = "/public/api/v1/posts";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private void insertPostsOfTwoUsers() {
		this.jdbcTemplate.update("""
				insert into tb_user (ID, EMAIL, PROVIDER, REGISTERED_DATE) values
				(201, 'owner@example.com', 'google', now()), (202, 'other@example.com', 'google', now())""");
		this.jdbcTemplate.update("""
				insert into tb_post (ID, TITLE, CONTENT, SLUG, TYPE, STATUS, AUTHOR_ID, CREATED_DATETIME, MODIFIED_DATETIME) values
				(21, 'KPI my draft', 'kpi', 'kpi-my-draft', 'post', 'draft', 201, '2026-11-06 00:00:00', '2026-11-06 00:00:00'),
				(22, 'KPI my private', 'kpi', 'kpi-my-private', 'post', 'private', 201, '2026-11-07 00:00:00', '2026-11-07 00:00:00'),
				(23, 'KPI their draft', 'kpi', 'kpi-their-draft', 'post', 'draft', 202, '2026-11-08 00:00:00', '2026-11-08 00:00:00'),
				(24, 'KPI my page', 'kpi', 'kpi-my-page', 'page', 'draft', 201, '2026-11-09 00:00:00', '2026-11-09 00:00:00')""");
	}

	@Test
	void anAnonymousVisitorDoesNotSeeUnpublishedPosts() throws Exception {
		insertPostsOfTwoUsers();
		this.mockMvc.perform(get(POSTS).param("query", "kpi"))
			.andExpect(jsonPath("$.posts[*].id", contains(2, 1)));
	}

	@Test
	void aSignedInUserAlsoSeesTheirOwnUnpublishedPostsButNotOthersOrPages() throws Exception {
		insertPostsOfTwoUsers();
		this.mockMvc.perform(get(POSTS).param("query", "kpi").with(TestUsers.signedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.posts[*].id", contains(22, 21, 2, 1)));
	}

	@Test
	void listsPublicPostsMatchingTitleOrContentIgnoringCaseNewestFirst() throws Exception {
		this.mockMvc.perform(get(POSTS).param("queryType", "keyword").param("query", "KpI"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.posts.length()").value(2))
			.andExpect(jsonPath("$.posts[0].id").value(2))
			.andExpect(jsonPath("$.posts[0].title").value("New one"))
			.andExpect(jsonPath("$.posts[0].slug").value("new-one"))
			.andExpect(jsonPath("$.posts[0].date").value("2026-11-05T09:45:11"))
			.andExpect(jsonPath("$.posts[1].id").value(1));
	}

	@Test
	void defaultsToKeywordWhenQueryTypeIsAbsent() throws Exception {
		this.mockMvc.perform(get(POSTS).param("query", "unrelated"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.posts.length()").value(1))
			.andExpect(jsonPath("$.posts[0].id").value(3));
	}

	/** Spring treats an empty parameter value like an absent one and applies the default. */
	@Test
	void defaultsToKeywordWhenQueryTypeIsEmpty() throws Exception {
		this.mockMvc.perform(get(POSTS).param("queryType", "").param("query", "unrelated"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.posts[0].id").value(3));
	}

	@Test
	void matchesLikeWildcardsLiterally() throws Exception {
		this.mockMvc.perform(get(POSTS).param("query", "100%"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.posts.length()").value(1))
			.andExpect(jsonPath("$.posts[0].id").value(2));
	}

	@Test
	void answersAnEmptyListWhenNothingMatches() throws Exception {
		this.mockMvc.perform(get(POSTS).param("query", "no such text"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.posts.length()").value(0));
	}

	@Test
	void listsEveryPublicPostNewestFirstWhenQueryIsAbsent() throws Exception {
		this.mockMvc.perform(get(POSTS))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.posts.length()").value(3))
			.andExpect(jsonPath("$.posts[0].id").value(2))
			.andExpect(jsonPath("$.posts[1].id").value(3))
			.andExpect(jsonPath("$.posts[2].id").value(1));
	}

	@Test
	void keepsTheNewestPostsWhenLimited() throws Exception {
		this.mockMvc.perform(get(POSTS).param("limit", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.posts.length()").value(2))
			.andExpect(jsonPath("$.posts[0].id").value(2))
			.andExpect(jsonPath("$.posts[1].id").value(3));
	}

	@Test
	void limitsASearchToo() throws Exception {
		this.mockMvc.perform(get(POSTS).param("query", "kpi").param("limit", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.posts.length()").value(1))
			.andExpect(jsonPath("$.posts[0].id").value(2));
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "-1", "101", "abc", "1.5" })
	void rejectsALimitOutsideTheSpec(String limit) throws Exception {
		this.mockMvc.perform(get(POSTS).param("limit", limit)).andExpect(status().isBadRequest());
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   " })
	void rejectsABlankQuery(String query) throws Exception {
		this.mockMvc.perform(get(POSTS).param("query", query)).andExpect(status().isBadRequest());
	}

	@ParameterizedTest
	@ValueSource(strings = { "KEYWORD", "Tag", "title" })
	void rejectsAQueryTypeOutsideTheSpec(String queryType) throws Exception {
		this.mockMvc.perform(get(POSTS).param("queryType", queryType).param("query", "kpi"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void answersNotImplementedForTag() throws Exception {
		this.mockMvc.perform(get(POSTS).param("queryType", "tag").param("query", "kpi"))
			.andExpect(status().isNotImplemented());
	}

}
