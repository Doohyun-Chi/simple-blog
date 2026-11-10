package com.simpleblog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs against the in-memory H2 database from src/test/resources/config. Each test inserts the fixture inside its
 * own transaction, which is rolled back afterwards, so no rows leak into other tests or test classes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql(statements = """
		insert into wp_posts (ID, post_title, post_content, post_password, post_name, post_type, post_status,
		  post_parent, menu_order, post_date, post_modified, post_date_gmt, post_modified_gmt) values
		(1, 'KPI', '<p>body</p>', '', 'kpi', 'post', 'publish', 0, 0,
		  '2026-11-05 09:45:11', '2026-11-06 10:00:00', '2026-11-05 00:45:11', '2026-11-06 01:00:00'),
		(2, 'Draft', 'draft body', '', 'a-draft', 'post', 'draft', 0, 0,
		  '2026-01-01 00:00:00', '2026-01-01 00:00:00', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
		(3, 'Private', 'private body', '', 'a-private', 'post', 'private', 0, 0,
		  '2026-01-01 00:00:00', '2026-01-01 00:00:00', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
		(4, 'Protected', 'secret body', 'pw', 'a-protected', 'post', 'publish', 0, 0,
		  '2026-01-01 00:00:00', '2026-01-01 00:00:00', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
		(5, 'About', 'page body', '', 'about', 'page', 'publish', 0, 0,
		  '2026-01-01 00:00:00', '2026-01-01 00:00:00', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
		(6, '큰기업', 'korean body', '', '%ed%81%b0%ea%b8%b0%ec%97%85-kpi', 'post', 'publish', 0, 0,
		  '2026-01-01 00:00:00', '2026-01-01 00:00:00', '2026-01-01 00:00:00', '2026-01-01 00:00:00');
		""")
class PostControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsThePublishedPostBodyAsInTheSpec() throws Exception {
		this.mockMvc.perform(get("/public/api/v1/post/kpi"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("KPI"))
			.andExpect(jsonPath("$.content").value("<p>body</p>"))
			.andExpect(jsonPath("$.status").value("publish"))
			.andExpect(jsonPath("$.date").value("2026-11-05T09:45:11"))
			.andExpect(jsonPath("$.modified").value("2026-11-06T10:00:00"));
	}

	@Test
	void findsANonAsciiSlugStoredPercentEncodedByWordPress() throws Exception {
		this.mockMvc.perform(get("/public/api/v1/post/{slug}", "큰기업-kpi"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").value("korean body"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "no-such-post" })
	void answersNotFoundForAnUnknownSlug(String slug) throws Exception {
		this.mockMvc.perform(get("/public/api/v1/post/{slug}", slug)).andExpect(status().isNotFound());
	}

}
