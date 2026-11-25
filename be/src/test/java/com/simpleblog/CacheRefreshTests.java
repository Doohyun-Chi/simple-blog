package com.simpleblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.simpleblog.component.CacheRefresh;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** The feed and the sitemap are dropped when a post is created, updated or deleted, and only once that change is committed. */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = """
		delete from tb_post;
		delete from tb_user;
		insert into tb_user (ID, EMAIL, PROVIDER, REGISTERED_DATE) values (1, 'owner@example.com', 'google', now());
		insert into tb_post (ID, TITLE, CONTENT, SLUG, TYPE, STATUS, AUTHOR_ID, CREATED_DATETIME, MODIFIED_DATETIME) values
		(1, 'KPI', 'old', 'kpi', 'post', 'publish', 1, '2026-11-05 00:45:11', '2026-11-06 01:00:00');
		""")
class CacheRefreshTests {

	private static final String BODY = "{\"title\": \"T\", \"slug\": \"%s\", \"content\": \"c\"}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private PlatformTransactionManager transactions;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoSpyBean
	private CacheRefresh cacheRefresh;

	/** These tests commit, so they leave no rows behind for the tests that roll back. */
	@AfterEach
	void removeCommittedRows() {
		this.jdbcTemplate.update("delete from tb_post");
		this.jdbcTemplate.update("delete from tb_user");
	}

	private void putInBoth() {
		this.cacheManager.getCache("feed").put("key", "cached");
		this.cacheManager.getCache("sitemap").put("key", "cached");
	}

	/** Whether both the feed and the sitemap are still cached; false when either is gone (they are dropped together). */
	private boolean kept() {
		Cache feed = this.cacheManager.getCache("feed");
		Cache sitemap = this.cacheManager.getCache("sitemap");
		boolean feedKept = feed.get("key") != null;
		assertThat(sitemap.get("key") != null).isEqualTo(feedKept);
		return feedKept;
	}

	@Test
	void dropsTheFeedOnlyWhenTheTransactionCommits() {
		TransactionTemplate template = new TransactionTemplate(this.transactions);
		putInBoth();
		template.executeWithoutResult((status) -> {
			this.cacheRefresh.afterPostChange();
			assertThat(kept()).isTrue();
		});
		assertThat(kept()).isFalse();
		putInBoth();
		template.executeWithoutResult((status) -> {
			this.cacheRefresh.afterPostChange();
			status.setRollbackOnly();
		});
		assertThat(kept()).isTrue();
	}

	@Test
	void dropsTheFeedWhenAPostIsCreatedUpdatedAndDeleted() throws Exception {
		putInBoth();
		this.mockMvc.perform(post("/public/api/v1/post").with(TestUsers.signedIn()).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(BODY.formatted("fresh"))).andExpect(status().isCreated());
		assertThat(kept()).isFalse();
		putInBoth();
		this.mockMvc.perform(put("/public/api/v1/post/{slug}", "fresh").with(TestUsers.signedIn()).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(BODY.formatted("fresh"))).andExpect(status().isOk());
		assertThat(kept()).isFalse();
		putInBoth();
		this.mockMvc.perform(delete("/public/api/v1/post/{slug}", "fresh").with(TestUsers.signedIn()).with(csrf()))
			.andExpect(status().is2xxSuccessful());
		assertThat(kept()).isFalse();
		verify(this.cacheRefresh, times(3)).afterPostChange();
	}

}
