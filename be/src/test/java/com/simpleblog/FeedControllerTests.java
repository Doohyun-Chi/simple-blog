package com.simpleblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.StringReader;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilderFactory;

import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.InputSource;

/** GET /public/feed against H2: published posts only, titles and links only, newest 20, and the 24 hour local cache. */
@SpringBootTest(properties = "simpleblog.auth.frontend-url=https://www.chidoo.me/")
@AutoConfigureMockMvc
@Transactional
@Sql(statements = """
		insert into tb_user (ID, EMAIL, PROVIDER, REGISTERED_DATE) values (1, 'owner@example.com', 'google', now());
		insert into tb_post (ID, TITLE, CONTENT, SLUG, TYPE, STATUS, AUTHOR_ID, CREATED_DATETIME, MODIFIED_DATETIME) values
		(1, 'A <b>"quoted"</b> & title', 'secret body text', 'kpi', 'post', 'publish', 1, '2026-11-05 00:45:11', '2026-11-06 01:00:00'),
		(2, 'Draft', 'draft body', 'a-draft', 'post', 'draft', 1, '2026-12-01 00:00:00', '2026-12-01 00:00:00'),
		(3, 'Private', 'private body', 'a-private', 'post', 'private', 1, '2026-12-01 00:00:00', '2026-12-01 00:00:00'),
		(4, 'About', 'no image', 'about', 'page', 'publish', 1, '2026-12-01 00:00:00', '2026-12-01 00:00:00'),
		(5, '큰기업', 'body', '%ed%81%b0%ea%b8%b0%ec%97%85-kpi', 'post', 'publish', 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00');
		""")
class FeedControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CacheManager cacheManager;

	@BeforeEach
	void startWithAnEmptyCache() {
		this.cacheManager.getCache("feed").clear();
	}

	private String feed() throws Exception {
		return this.mockMvc.perform(get("/public/feed"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_ATOM_XML))
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	@Test
	void escapesOnlyWhatXmlPredefinesSoTheFeedStaysParsableWithAccentsQuotesAndControlCharacters() throws Exception {
		this.jdbcTemplate.update("update tb_post set TITLE = ? where ID = 5", "Caf\u00e9 \u201cquote\u201d \u2014 it's\u0001 \ud83d\ude00");
		String feed = feed();
		assertThat(feed).contains("<title>Caf\u00e9 \u201cquote\u201d \u2014 it&apos;s \ud83d\ude00</title>").doesNotContain("&eacute;", "&mdash;");
		DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(feed)));
	}

	@Test
	void listsOnlyThePublishedPostsNewestFirstWithTheirTitleAndLinkAndNoContent() throws Exception {
		String feed = feed();
		assertThat(feed).startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<feed xmlns=\"http://www.w3.org/2005/Atom\">")
			.contains("<title>Dreaming for the Future</title>")
			.contains("<link rel=\"self\" href=\"http://localhost/public/feed\" />")
			.contains("<link rel=\"alternate\" href=\"https://www.chidoo.me/\" />")
			.contains("<id>https://www.chidoo.me/post/kpi</id>")
			.contains("<title>A &lt;b&gt;&quot;quoted&quot;&lt;/b&gt; &amp; title</title>")
			.contains("<title>큰기업</title>")
			.contains("<link rel=\"alternate\" href=\"https://www.chidoo.me/post/%ED%81%B0%EA%B8%B0%EC%97%85-kpi\" />")
			.contains("<published>2026-11-05T09:45:11+09:00</published>")
			.contains("<updated>2026-11-06T10:00:00+09:00</updated>")
			.doesNotContain("Draft", "Private", "About", "secret body", "<summary", "<content");
		assertThat(feed.split("<entry>").length - 1).isEqualTo(2);
		assertThat(feed.indexOf("/post/kpi")).isLessThan(feed.indexOf("%ED%81%B0"));
	}

	@Test
	void takesTheFeedUpdatedFromTheFirstEntry() throws Exception {
		assertThat(feed()).contains("<link rel=\"self\" href=\"http://localhost/public/feed\" />\n  <updated>2026-11-06T10:00:00+09:00</updated>\n  <entry>");
	}

	@Test
	void listsOnlyTheNewest20() throws Exception {
		for (int i = 100; i < 125; i++) {
			this.jdbcTemplate.update("""
					insert into tb_post (ID, TITLE, CONTENT, SLUG, TYPE, STATUS, AUTHOR_ID, CREATED_DATETIME, MODIFIED_DATETIME)
					values (?, ?, 'c', ?, 'post', 'publish', 1, '2027-01-01 00:00:00', '2027-01-01 00:00:00')""", i, "n" + i, "n-" + i);
		}
		String feed = feed();
		assertThat(feed.split("<entry>").length - 1).isEqualTo(20);
		assertThat(feed).contains("<title>n124</title>").contains("<title>n105</title>").doesNotContain("<title>n104</title>", "kpi");
	}

	@Test
	void hasNoEntryWhenNothingIsPublished() throws Exception {
		this.jdbcTemplate.update("update tb_post set STATUS = 'draft'");
		assertThat(feed()).contains("<updated>").doesNotContain("<entry>");
	}

	@Test
	void keepsServingTheCachedFeedUntilItExpires() throws Exception {
		assertThat(feed()).doesNotContain("brand new");
		this.jdbcTemplate.update("""
				insert into tb_post (ID, TITLE, CONTENT, SLUG, TYPE, STATUS, AUTHOR_ID, CREATED_DATETIME, MODIFIED_DATETIME)
				values (50, 'brand new', 'c', 'brand-new', 'post', 'publish', 1, '2027-01-01 00:00:00', '2027-01-01 00:00:00')""");
		assertThat(feed()).doesNotContain("brand new");
		this.cacheManager.getCache("feed").clear();
		assertThat(feed()).contains("brand new");
	}

	@Test
	void expiresTheCachedFeedTwentyFourHoursAfterItWasLoaded() {
		Cache<Object, Object> cache = ((CaffeineCache) this.cacheManager.getCache("feed")).getNativeCache();
		assertThat(cache.policy().expireAfterWrite().orElseThrow().getExpiresAfter(TimeUnit.HOURS)).isEqualTo(24);
	}

}
