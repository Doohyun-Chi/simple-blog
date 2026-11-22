package com.simpleblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * PostInput.replacedMedia: the content images a post save replaced are deleted, row and file, once nothing refers to them. A
 * file is removed only when the transaction commits, so unlike the other tests these commit, and every row is removed
 * after each test. The post kpi shows flow-old; the draft elsewhere shows shared and a slug that merely begins with gone.
 * library is an internal image of the media library that nothing shows.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = """
		insert into tb_user (ID, EMAIL, PROVIDER, REGISTERED_DATE) values
		(1, 'owner@example.com', 'google', now()),
		(2, 'other@example.com', 'google', now());
		insert into tb_post (ID, TITLE, CONTENT, SLUG, TYPE, STATUS, AUTHOR_ID, CREATED_DATETIME, MODIFIED_DATETIME) values
		(1001, 'KPI', '![d](http://localhost:8080/public/api/v1/media/flow-old11111)', 'kpi', 'post', 'publish', 1,
		 '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
		(1002, 'Elsewhere', '<img src="/public/api/v1/media/shared-33333333"> /public/api/v1/media/gone-55555555-v2', 'elsewhere',
		 'post', 'draft', 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00');
		insert into tb_media (ID, SLUG, PROVIDER, MIME_TYPE, LOCATION, THUMBNAIL, "date", OWNER_ID) values
		(9001, 'flow-old11111', 'content', 'image/png', 'flow-old.png', null, now(), 1),
		(9002, 'flow-new22222', 'content', 'image/png', 'flow-new.png', null, now(), 1),
		(9003, 'shared-33333333', 'content', 'image/png', 'shared.png', null, now(), 1),
		(9004, 'theirs-44444444', 'content', 'image/png', 'theirs.png', null, now(), 2),
		(9005, 'youtube-abc', 'external', 'https://www.youtube.com/watch?v=abc', 'https://www.youtube.com/watch?v=abc', null, now(), 1),
		(9006, 'gone-55555555', 'content', 'image/png', 'gone.png', null, now(), 1),
		(9007, 'library-77777777', 'internal', 'image/png', 'library.png', null, now(), 1);
		""")
@Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, statements = """
		delete from tb_media;
		delete from tb_post;
		delete from tb_user;
		""")
class PostMediaCleanupTests {

	private static final List<String> FILES = List.of("flow-old.png", "flow-new.png", "shared.png", "theirs.png", "gone.png", "library.png");

	private static final String ALL = "\"flow-old11111\", \"flow-new22222\", \"shared-33333333\", \"theirs-44444444\", \"youtube-abc\", "
			+ "\"gone-55555555\", \"library-77777777\", \"no-such-media\"";

	@TempDir
	static Path directory;

	@DynamicPropertySource
	static void storage(DynamicPropertyRegistry registry) {
		registry.add("simpleblog.media.directory", () -> directory.toString());
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void storeFiles() throws IOException {
		for (String name : FILES) {
			Files.write(directory.resolve(name), new byte[] { 1 });
		}
	}

	private static String body(String slug, String content, String replacedMedia) {
		return """
				{"title": "T", "slug": "%s", "content": "%s", "replacedMedia": [%s]}""".formatted(slug, content, replacedMedia);
	}

	private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
		return request.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private List<String> slugs() {
		return this.jdbcTemplate.queryForList("select slug from tb_media order by id", String.class);
	}

	private List<String> files() throws IOException {
		try (var listing = Files.list(directory)) {
			return listing.map((file) -> file.getFileName().toString()).sorted().toList();
		}
	}

	@Test
	void deletesTheReplacedContentImagesOfTheWriterThatNothingShowsAnyMoreButNotTheLibrarys() throws Exception {
		String content = "![d](http://localhost:8080/public/api/v1/media/flow-new22222 \\\"diagram\\\")";
		this.mockMvc.perform(json(put("/public/api/v1/post/kpi").with(TestUsers.signedIn()), body("kpi", content, ALL)))
			.andExpect(status().isOk());
		assertThat(slugs()).containsExactly("flow-new22222", "shared-33333333", "theirs-44444444", "youtube-abc", "library-77777777");
		assertThat(files()).containsExactly("flow-new.png", "library.png", "shared.png", "theirs.png");
	}

	@Test
	void checksEveryPostSoACreatedPostDoesNotTakeAnImageAnotherPostShows() throws Exception {
		this.mockMvc.perform(json(post("/public/api/v1/post").with(TestUsers.signedIn()), body("fresh", "none", ALL)))
			.andExpect(status().isCreated());
		assertThat(slugs()).containsExactly("flow-old11111", "shared-33333333", "theirs-44444444", "youtube-abc", "library-77777777");
		assertThat(files()).containsExactly("flow-old.png", "library.png", "shared.png", "theirs.png");
	}

	@Test
	void deletesNothingWhenTheSaveIsRefused() throws Exception {
		this.mockMvc.perform(json(put("/public/api/v1/post/kpi").with(TestUsers.signedIn("other@example.com")), body("kpi", "x", ALL)))
			.andExpect(status().isForbidden());
		String tooMany = String.join(", ", Collections.nCopies(101, "\"gone-55555555\""));
		this.mockMvc.perform(json(put("/public/api/v1/post/kpi").with(TestUsers.signedIn()), body("kpi", "x", tooMany)))
			.andExpect(status().isBadRequest());
		assertThat(slugs()).hasSize(7);
		assertThat(files()).hasSameElementsAs(FILES);
	}

}
