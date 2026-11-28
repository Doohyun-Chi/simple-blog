package com.simpleblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

/**
 * DELETE /public/api/v1/media/{slug} removes the stored file, which happens once the transaction commits, so these tests
 * do not run in a transaction of their own, like PostMediaCleanupTests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, statements = """
		delete from tb_media;
		delete from tb_user;
		""")
class MediaDeleteFileTests {

	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0 };

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

	@AfterEach
	void clearDirectory() throws IOException {
		for (String name : files()) {
			Files.delete(directory.resolve(name));
		}
	}

	private List<String> files() throws IOException {
		try (var listing = Files.list(directory)) {
			return listing.map((file) -> file.getFileName().toString()).toList();
		}
	}

	private void upload(String slug, String email, long userId) throws Exception {
		this.mockMvc.perform(multipart("/public/api/v1/media").file(new MockMultipartFile("file", "a.png", "image/png", PNG))
			.param("slug", slug).with(TestUsers.signedInWithId(email, userId)).with(csrf())).andExpect(status().isCreated());
	}

	@Test
	void removesTheRowAndTheStoredFileOfTheOwner() throws Exception {
		upload("team-photo", "a@example.com", 101);
		assertThat(files()).hasSize(1);
		this.mockMvc.perform(delete("/public/api/v1/media/team-photo").with(TestUsers.signedInWithId("a@example.com", 101)).with(csrf()))
			.andExpect(status().isNoContent());
		assertThat(files()).isEmpty();
		assertThat(this.jdbcTemplate.queryForList("select slug from tb_media", String.class)).isEmpty();
	}

	private void uploadDiagram(String slug, String source, boolean replace) throws Exception {
		this.mockMvc.perform(multipart("/public/api/v1/media").file(new MockMultipartFile("file", "a.png", "image/png", PNG))
			.param("provider", "content").param("slug", slug).param("replace", String.valueOf(replace))
			.param("source", source).param("sourceType", "drawio")
			.with(TestUsers.signedInWithId("a@example.com", 101)).with(csrf())).andExpect(status().is2xxSuccessful());
	}

	@Test
	void removesTheSourceBesideTheFile() throws Exception {
		uploadDiagram("flow", "<mxfile/>", false);
		assertThat(files()).hasSize(2).anyMatch((name) -> name.endsWith(".drawio"));
		this.mockMvc.perform(delete("/public/api/v1/media/flow").with(TestUsers.signedInWithId("a@example.com", 101)).with(csrf()))
			.andExpect(status().isNoContent());
		assertThat(files()).isEmpty();
	}

	@Test
	void removesTheOldFileAndSourceOnceReplaced() throws Exception {
		uploadDiagram("flow", "<mxfile/>", false);
		List<String> before = files();
		uploadDiagram("flow", "<mxfile><diagram/></mxfile>", true);
		assertThat(files()).hasSize(2).doesNotContainAnyElementsOf(before);
		this.mockMvc.perform(multipart("/public/api/v1/media").file(new MockMultipartFile("file", "a.png", "image/png", PNG))
			.param("slug", "flow").param("replace", "true")
			.with(TestUsers.signedInWithId("a@example.com", 101)).with(csrf())).andExpect(status().isOk());
		assertThat(files()).hasSize(1).noneMatch((name) -> name.endsWith(".drawio"));
	}

	@Test
	void keepsTheRowAndTheFileOfAnotherUser() throws Exception {
		upload("team-photo", "a@example.com", 101);
		this.mockMvc.perform(delete("/public/api/v1/media/team-photo").with(TestUsers.signedInWithId("b@example.com", 102)).with(csrf()))
			.andExpect(status().isForbidden());
		assertThat(files()).hasSize(1);
		assertThat(this.jdbcTemplate.queryForList("select slug from tb_media", String.class)).containsExactly("team-photo");
	}

}
