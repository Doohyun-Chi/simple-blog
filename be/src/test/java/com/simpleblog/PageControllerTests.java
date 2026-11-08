package com.simpleblog;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
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
		insert into wp_posts (ID, post_title, post_name, post_type, post_status, post_parent, menu_order,
		  post_date_gmt, post_modified_gmt) values
		(10, 'Reading', 'reading', 'page', 'publish', 0, 1, '2017-06-04 14:29:56', '2019-09-22 11:08:17'),
		(11, 'Reading 2019', 'reading-2019', 'page', 'publish', 10, 1, '2019-02-07 00:41:58', '2020-04-16 15:27:54'),
		(2, 'About', 'about', 'page', 'publish', 0, 0, '2016-03-14 02:36:24', '2024-03-01 23:57:29'),
		(20, 'Draft page', 'draft', 'page', 'draft', 0, 0, '2020-01-01 00:00:00', '2020-01-01 00:00:00'),
		(30, 'A blog post', 'a-post', 'post', 'publish', 0, 0, '2020-01-01 00:00:00', '2020-01-01 00:00:00');
		""")
class PageControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void listsOnlyPublishedPagesOrderedByMenuOrderThenId() throws Exception {
		this.mockMvc.perform(get("/public/api/v1/pages"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.pages", hasSize(3)))
			.andExpect(jsonPath("$.pages[*].id", contains(2, 10, 11)));
	}

	@Test
	void mapsPageFieldsAsInTheSpec() throws Exception {
		this.mockMvc.perform(get("/public/api/v1/pages"))
			.andExpect(jsonPath("$.pages[0].title").value("About"))
			.andExpect(jsonPath("$.pages[0].slug").value("about"))
			.andExpect(jsonPath("$.pages[0].parentId").value(nullValue()))
			.andExpect(jsonPath("$.pages[0].menuOrder").value(0))
			.andExpect(jsonPath("$.pages[0].publishedAt").value("2016-03-14T02:36:24Z"))
			.andExpect(jsonPath("$.pages[0].modifiedAt").value("2024-03-01T23:57:29Z"))
			.andExpect(jsonPath("$.pages[2].parentId").value(10));
	}

}
