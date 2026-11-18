package com.simpleblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** POST /public/api/v1/media (image upload and YouTube registration) and GET /public/api/v1/media/{slug} against H2. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MediaControllerTests {

	private static final String MEDIA = "/public/api/v1/media";

	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0 };

	private static final byte[] WEBP = { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P' };

	@TempDir
	static Path directory;

	@DynamicPropertySource
	static void storage(DynamicPropertyRegistry registry) {
		registry.add("simpleblog.media.directory", () -> directory.toString());
	}

	@Autowired
	private MockMvc mockMvc;

	private RequestBuilder upload(String name, String declaredType, byte[] bytes) {
		return multipart(MEDIA).file(new MockMultipartFile("file", name, declaredType, bytes)).with(TestUsers.signedIn()).with(csrf());
	}

	private RequestBuilder link(String url) {
		return post(MEDIA).with(TestUsers.signedIn()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"url\": \"%s\"}".formatted(url));
	}

	@Test
	void storesAnImageAndServesItBack() throws Exception {
		String body = this.mockMvc.perform(upload("My Photo (1).PNG", "image/png", PNG))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.provider").value("internal"))
			.andExpect(jsonPath("$.mimeType").value("image/png"))
			.andExpect(jsonPath("$.thumbnail").doesNotExist())
			.andReturn().getResponse().getContentAsString();
		String slug = JsonPath.read(body, "$.slug");
		assertThat(slug).matches("my-photo-1-[0-9a-f]{8}");
		assertThat(JsonPath.<String>read(body, "$.url")).isEqualTo("/public/api/v1/media/" + slug);
		this.mockMvc.perform(get("/public/api/v1/media/" + slug))
			.andExpect(status().isOk())
			.andExpect(content().contentType("image/png"))
			.andExpect(content().bytes(PNG));
	}

	@Test
	void decidesTheTypeFromTheContentNotFromTheClient() throws Exception {
		this.mockMvc.perform(upload("evil.html", "text/html", PNG)).andExpect(status().isCreated()).andExpect(jsonPath("$.mimeType").value("image/png"));
		this.mockMvc.perform(upload("a.png", "image/png", "<script>alert(1)</script>".getBytes())).andExpect(status().isUnsupportedMediaType());
		this.mockMvc.perform(upload("a.webp", "image/webp", WEBP)).andExpect(status().isCreated()).andExpect(jsonPath("$.mimeType").value("image/webp"));
		this.mockMvc.perform(upload("a.webp", "image/webp", new byte[] { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E' }))
			.andExpect(status().isUnsupportedMediaType());
	}

	@Test
	void namesAFileWithoutLatinLettersMedia() throws Exception {
		this.mockMvc.perform(upload("사진.png", "image/png", PNG)).andExpect(status().isCreated()).andExpect(jsonPath("$.slug").value(org.hamcrest.Matchers.matchesPattern("media-[0-9a-f]{8}")));
	}

	@Test
	void refusesAnEmptyFileAndAMissingFile() throws Exception {
		this.mockMvc.perform(upload("a.png", "image/png", new byte[0])).andExpect(status().isBadRequest());
		this.mockMvc.perform(multipart(MEDIA).with(TestUsers.signedIn()).with(csrf())).andExpect(status().is4xxClientError());
	}

	@Test
	void registersAYouTubeVideoByReferenceWithItsThumbnail() throws Exception {
		this.mockMvc.perform(link("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10s"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.slug").value("youtube-dQw4w9WgXcQ"))
			.andExpect(jsonPath("$.provider").value("external"))
			.andExpect(jsonPath("$.mimeType").value("https://youtu.be/dQw4w9WgXcQ"))
			.andExpect(jsonPath("$.url").value("https://youtu.be/dQw4w9WgXcQ"))
			.andExpect(jsonPath("$.thumbnail").value("https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg"));
		this.mockMvc.perform(link("https://youtu.be/dQw4w9WgXcQ?si=abc"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.slug").value("youtube-dQw4w9WgXcQ"));
		this.mockMvc.perform(get(MEDIA + "/youtube-dQw4w9WgXcQ")).andExpect(status().isNotFound());
	}

	@Test
	void acceptsTheOtherYouTubeLinkForms() throws Exception {
		for (String url : new String[] { "https://youtu.be/AAAAAAAAAAA", "https://m.youtube.com/watch?feature=x&v=BBBBBBBBBBB",
				"https://www.youtube.com/shorts/CCCCCCCCCCC", "https://www.youtube.com/embed/DDDDDDDDDDD" }) {
			this.mockMvc.perform(link(url)).andExpect(status().isCreated());
		}
	}

	@Test
	void refusesLinksThatAreNotOneYouTubeVideo() throws Exception {
		for (String url : new String[] { "", "not a url", "https://example.com/watch?v=dQw4w9WgXcQ", "https://youtube.com.evil.test/watch?v=dQw4w9WgXcQ",
				"https://www.youtube.com/watch?v=short", "https://www.youtube.com/", "javascript:alert(1)", "https://youtu.be/dQw4w9WgXcQ/extra" }) {
			this.mockMvc.perform(link(url)).andExpect(status().isBadRequest());
		}
	}

	@Test
	void needsASignedInSessionAndACsrfToken() throws Exception {
		this.mockMvc.perform(multipart(MEDIA).file(new MockMultipartFile("file", "a.png", "image/png", PNG)).with(csrf()))
			.andExpect(status().isUnauthorized());
		this.mockMvc.perform(multipart(MEDIA).file(new MockMultipartFile("file", "a.png", "image/png", PNG)).with(TestUsers.signedIn()))
			.andExpect(status().isForbidden());
		this.mockMvc.perform(post(MEDIA).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"url\": \"https://youtu.be/dQw4w9WgXcQ\"}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void answersNotFoundForAnUnknownSlug() throws Exception {
		this.mockMvc.perform(get(MEDIA + "/nothing-here")).andExpect(status().isNotFound());
		this.mockMvc.perform(get(MEDIA + "/..%2F..%2Fetc%2Fpasswd")).andExpect(status().is4xxClientError());
	}

}
