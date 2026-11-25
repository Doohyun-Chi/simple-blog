package com.simpleblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import com.simpleblog.component.CacheRefresh;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.xml.sax.InputSource;

/** GET /public/sitemap.xml against H2: published posts only, cached, and dropped on a post change. */
@SpringBootTest(properties = "simpleblog.auth.frontend-url=https://www.chidoo.me/")
@AutoConfigureMockMvc
@Sql(statements = """
		delete from tb_post;
		delete from tb_user;
		insert into tb_user (ID, EMAIL, PROVIDER, REGISTERED_DATE) values (1, 'owner@example.com', 'google', now());
		insert into tb_post (ID, TITLE, CONTENT, SLUG, TYPE, STATUS, AUTHOR_ID, CREATED_DATETIME, MODIFIED_DATETIME) values
		(1, 'KPI & more', 'secret body text', 'kpi&more', 'post', 'publish', 1, '2026-11-05 00:45:11', '2026-11-06 01:00:00'),
		(2, 'Draft', 'draft body', 'a-draft', 'post', 'draft', 1, '2026-12-01 00:00:00', '2026-12-01 00:00:00'),
		(3, 'Private', 'private body', 'a-private', 'post', 'private', 1, '2026-12-01 00:00:00', '2026-12-01 00:00:00'),
		(4, 'About', 'no image', 'about', 'page', 'publish', 1, '2026-12-01 00:00:00', '2026-12-01 00:00:00'),
		(5, '큰기업', 'body', '%ed%81%b0%ea%b8%b0%ec%97%85-kpi', 'post', 'publish', 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00');
		""")
class SitemapControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private CacheRefresh cacheRefresh;

	@BeforeEach
	void startWithAnEmptyCache() {
		this.cacheManager.getCache("sitemap").clear();
	}

	/** The rows are committed, so they are removed by hand. */
	@AfterEach
	void removeCommittedRows() {
		this.jdbcTemplate.update("delete from tb_post");
		this.jdbcTemplate.update("delete from tb_user");
	}

	private String sitemap() throws Exception {
		return this.mockMvc.perform(get("/public/sitemap.xml"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	@Test
	void listsTheHomePageAndOnlyThePublishedPostsNewestModifiedFirstAsParsableXml() throws Exception {
		String sitemap = sitemap();
		assertThat(sitemap).startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">")
			.contains("<loc>https://www.chidoo.me/</loc>\n  </url>")
			.contains("<loc>https://www.chidoo.me/post/kpi&amp;more</loc>\n    <lastmod>2026-11-06T01:00:00Z</lastmod>")
			.contains("<loc>https://www.chidoo.me/post/%ED%81%B0%EA%B8%B0%EC%97%85-kpi</loc>\n    <lastmod>2026-01-01T00:00:00Z</lastmod>")
			.doesNotContain("a-draft", "a-private", "about");
		assertThat(sitemap.indexOf("kpi&amp;more")).isLessThan(sitemap.indexOf("%ED%81%B0"));
		DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(sitemap)));
	}

	@Test
	void showsANewPostAfterTheCacheIsDroppedByTheChange() throws Exception {
		assertThat(sitemap()).doesNotContain("a-new-post");
		this.jdbcTemplate.update("""
				insert into tb_post (ID, TITLE, CONTENT, SLUG, TYPE, STATUS, AUTHOR_ID, CREATED_DATETIME, MODIFIED_DATETIME)
				values (9, 'New', 'c', 'a-new-post', 'post', 'publish', 1, '2026-12-02 00:00:00', '2026-12-02 00:00:00')""");
		assertThat(sitemap()).doesNotContain("a-new-post");
		this.cacheRefresh.afterPostChange();
		assertThat(sitemap()).contains("/post/a-new-post");
	}

	@Test
	void hasOnlyTheHomePageWhenNothingIsPublished() throws Exception {
		this.jdbcTemplate.update("delete from tb_post");
		assertThat(sitemap().split("<url>").length - 1).isEqualTo(1);
	}

}
