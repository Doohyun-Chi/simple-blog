package com.simpleblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** What a post or a media item records as its author or owner: the creator's id in TB_USER, registered on first use. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserRegistrationTests {

	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0 };

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	private long userId(String email) {
		return this.jdbc.queryForObject("select id from tb_user where email = ?", Long.class, email);
	}

	private void createPost(String email, String slug) throws Exception {
		this.mockMvc.perform(post("/public/api/v1/post").with(TestUsers.signedIn(email)).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\": \"t\", \"slug\": \"%s\", \"content\": \"c\"}".formatted(slug))).andExpect(status().isCreated());
	}

	@Test
	void aPostIsAuthoredByTheRegisteredUser() throws Exception {
		createPost("Owner@Example.com ", "first");
		long id = userId("owner@example.com");
		assertThat(this.jdbc.queryForObject("select author_id from tb_post where slug = 'first'", Long.class)).isEqualTo(id);
	}

	@Test
	void theSameUserIsRegisteredOnceAndAnotherGetsANewId() throws Exception {
		createPost(TestUsers.OWNER, "one");
		createPost(TestUsers.OWNER, "two");
		createPost("second@example.com", "three");
		assertThat(this.jdbc.queryForObject("select count(*) from tb_user", Integer.class)).isEqualTo(2);
		assertThat(userId("second@example.com")).isNotEqualTo(userId(TestUsers.OWNER));
		assertThat(this.jdbc.queryForObject("select count(distinct author_id) from tb_post where slug in ('one', 'two')", Integer.class))
			.isEqualTo(1);
	}

	@Test
	void anExistingUserKeepsItsIdAndProvider() throws Exception {
		this.jdbc.update("insert into tb_user (email, provider, registered_date) values ('owner@example.com', 'google', now())");
		long id = userId(TestUsers.OWNER);
		createPost(TestUsers.OWNER, "kept");
		assertThat(this.jdbc.queryForObject("select count(*) from tb_user", Integer.class)).isEqualTo(1);
		assertThat(this.jdbc.queryForObject("select author_id from tb_post where slug = 'kept'", Long.class)).isEqualTo(id);
	}

	@Test
	void aMediaItemIsOwnedByTheRegisteredUser() throws Exception {
		this.mockMvc.perform(multipart("/public/api/v1/media").file(new MockMultipartFile("file", "a.png", "image/png", PNG))
			.with(TestUsers.signedIn("media@example.com")).with(csrf())).andExpect(status().isCreated());
		this.mockMvc.perform(post("/public/api/v1/media").with(TestUsers.signedIn("media@example.com")).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"url\": \"https://youtu.be/dQw4w9WgXcQ\"}")).andExpect(status().isCreated());
		assertThat(this.jdbc.queryForList("select distinct owner_id from tb_media", Long.class)).containsExactly(userId("media@example.com"));
	}

}
