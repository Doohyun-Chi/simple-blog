package com.simpleblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.CRC32;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
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

	/** An upload as the editor sends a draw.io diagram, or another provider to see it refused. */
	private RequestBuilder upload(String name, byte[] bytes, String provider) {
		return multipart(MEDIA).file(new MockMultipartFile("file", name, "image/png", bytes)).param("provider", provider)
			.with(TestUsers.signedIn()).with(csrf());
	}

	/** An upload under the slug the writer typed, as the editor sends a pasted or dropped image. */
	private RequestBuilder uploadAs(String slug, byte[] bytes, boolean replace, RequestPostProcessor user) {
		return multipart(MEDIA).file(new MockMultipartFile("file", "image.png", "image/png", bytes)).param("slug", slug)
			.param("replace", String.valueOf(replace)).with(user).with(csrf());
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

	/** A PNG chunk: length, type, data and the CRC of type and data. */
	private static byte[] chunk(String type, byte[] data) {
		byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
		CRC32 crc = new CRC32();
		crc.update(typeBytes);
		crc.update(data);
		return ByteBuffer.allocate(12 + data.length).putInt(data.length).put(typeBytes).put(data).putInt((int) crc.getValue()).array();
	}

	/** A PNG as draw.io exports it with "Include a copy of my diagram": the diagram rides in a tEXt chunk named mxfile. */
	private static byte[] drawioPng() {
		ByteArrayOutputStream png = new ByteArrayOutputStream();
		png.writeBytes(new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A });
		png.writeBytes(chunk("IHDR", new byte[] { 0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0 }));
		png.writeBytes(chunk("tEXt", "mxfile\0%3Cmxfile%3E%3Cdiagram%20name%3D%22Page-1%22%3E...%3C%2Fdiagram%3E%3C%2Fmxfile%3E"
			.getBytes(StandardCharsets.ISO_8859_1)));
		png.writeBytes(chunk("IEND", new byte[0]));
		return png.toByteArray();
	}

	@Test
	void storesADrawioPngAsContentAndServesItByteForByteSoTheDiagramInsideSurvives() throws Exception {
		byte[] drawio = drawioPng();
		String body = this.mockMvc.perform(upload("flow.drawio.png", drawio, "content"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.provider").value("content"))
			.andExpect(jsonPath("$.mimeType").value("image/png"))
			.andReturn().getResponse().getContentAsString();
		this.mockMvc.perform(get(JsonPath.<String>read(body, "$.url")))
			.andExpect(status().isOk())
			.andExpect(content().bytes(drawio));
	}

	@ParameterizedTest
	@ValueSource(strings = { "external", "Content", "library" })
	void refusesAnUploadAsAProviderThatKeepsNoFileOrIsUnknown(String provider) throws Exception {
		this.mockMvc.perform(upload("flow.png", PNG, provider)).andExpect(status().isBadRequest());
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
		this.mockMvc.perform(upload("사진.png", "image/png", PNG))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.slug").value(org.hamcrest.Matchers.matchesPattern("media-[0-9a-f]{8}")));
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
	void listsEveryItemNewestFirstForASignedInUser() throws Exception {
		this.mockMvc.perform(get("/public/api/v1/medias").with(TestUsers.signedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.medias").isEmpty());
		this.mockMvc.perform(upload("first.png", "image/png", PNG)).andExpect(status().isCreated());
		this.mockMvc.perform(link("https://youtu.be/dQw4w9WgXcQ")).andExpect(status().isCreated());
		this.mockMvc.perform(upload("third.png", "image/png", PNG)).andExpect(status().isCreated());
		this.mockMvc.perform(get("/public/api/v1/medias").with(TestUsers.signedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.medias.length()").value(3))
			.andExpect(jsonPath("$.medias[0].slug").value(org.hamcrest.Matchers.startsWith("third-")))
			.andExpect(jsonPath("$.medias[1].slug").value("youtube-dQw4w9WgXcQ"))
			.andExpect(jsonPath("$.medias[1].thumbnail").value("https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg"))
			.andExpect(jsonPath("$.medias[2].slug").value(org.hamcrest.Matchers.startsWith("first-")))
			.andExpect(jsonPath("$.medias[2].url").value(org.hamcrest.Matchers.startsWith("/public/api/v1/media/first-")));
	}

	@Test
	void keepsTheListForSignedInUsersOnly() throws Exception {
		this.mockMvc.perform(get("/public/api/v1/medias")).andExpect(status().isUnauthorized());
	}

	@Test
	void answersNotFoundForAnUnknownSlug() throws Exception {
		this.mockMvc.perform(get(MEDIA + "/nothing-here")).andExpect(status().isNotFound());
		this.mockMvc.perform(get(MEDIA + "/..%2F..%2Fetc%2Fpasswd")).andExpect(status().is4xxClientError());
	}

	@Test
	void keepsTheSlugTheWriterGaveWithoutASuffix() throws Exception {
		this.mockMvc.perform(uploadAs("team-photo", PNG, false, TestUsers.signedIn()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.slug").value("team-photo"))
			.andExpect(jsonPath("$.url").value(MEDIA + "/team-photo"));
		this.mockMvc.perform(get(MEDIA + "/team-photo")).andExpect(status().isOk()).andExpect(content().bytes(PNG));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "Team-Photo", "team photo", "-team", "team-", "team--photo", "사진" })
	void refusesASlugOutsideTheRule(String slug) throws Exception {
		this.mockMvc.perform(uploadAs(slug, PNG, false, TestUsers.signedIn())).andExpect(status().isBadRequest());
	}

	@Test
	void refusesToReuseASlugOfTheWritersImageUnlessAskedToReplaceIt() throws Exception {
		this.mockMvc.perform(uploadAs("team-photo", PNG, false, TestUsers.signedIn())).andExpect(status().isCreated());
		this.mockMvc.perform(uploadAs("team-photo", WEBP, false, TestUsers.signedIn())).andExpect(status().isConflict());
		this.mockMvc.perform(get(MEDIA + "/team-photo")).andExpect(content().bytes(PNG));
	}

	@Test
	void replacesTheFileOfTheWritersImageKeepingItsSlugAndProvider() throws Exception {
		this.mockMvc.perform(upload("flow.png", PNG, "content")).andExpect(status().isCreated());
		String first = this.mockMvc.perform(uploadAs("team-photo", PNG, false, TestUsers.signedIn()))
			.andReturn().getResponse().getContentAsString();
		this.mockMvc.perform(uploadAs("team-photo", WEBP, true, TestUsers.signedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.slug").value("team-photo"))
			.andExpect(jsonPath("$.provider").value(JsonPath.<String>read(first, "$.provider")))
			.andExpect(jsonPath("$.mimeType").value("image/webp"));
		this.mockMvc.perform(get(MEDIA + "/team-photo")).andExpect(status().isOk()).andExpect(content().bytes(WEBP));
	}

	@Test
	void createsTheImageWhenReplacingASlugNobodyHas() throws Exception {
		this.mockMvc.perform(uploadAs("brand-new", PNG, true, TestUsers.signedIn())).andExpect(status().isCreated());
	}

	@Test
	void neverReplacesAnotherUsersImage() throws Exception {
		this.mockMvc.perform(uploadAs("team-photo", PNG, false, TestUsers.signedInWithId("a@example.com", 101))).andExpect(status().isCreated());
		this.mockMvc.perform(uploadAs("team-photo", WEBP, true, TestUsers.signedInWithId("b@example.com", 102)))
			.andExpect(status().isUnprocessableContent());
		this.mockMvc.perform(get(MEDIA + "/team-photo")).andExpect(content().bytes(PNG));
	}

	@Test
	void deletesTheOwnersImageWithItsFile() throws Exception {
		this.mockMvc.perform(uploadAs("team-photo", PNG, false, TestUsers.signedIn())).andExpect(status().isCreated());
		this.mockMvc.perform(delete(MEDIA + "/team-photo").with(TestUsers.signedIn()).with(csrf())).andExpect(status().isNoContent());
		this.mockMvc.perform(get(MEDIA + "/team-photo")).andExpect(status().isNotFound());
		this.mockMvc.perform(get("/public/api/v1/medias").with(TestUsers.signedIn())).andExpect(jsonPath("$.medias.length()").value(0));
	}

	@Test
	void deletesTheOwnersVideoRow() throws Exception {
		this.mockMvc.perform(link("https://youtu.be/dQw4w9WgXcQ")).andExpect(status().isCreated());
		this.mockMvc.perform(delete(MEDIA + "/youtube-dQw4w9WgXcQ").with(TestUsers.signedIn()).with(csrf())).andExpect(status().isNoContent());
		this.mockMvc.perform(get("/public/api/v1/medias").with(TestUsers.signedIn())).andExpect(jsonPath("$.medias.length()").value(0));
	}

	@Test
	void keepsAnotherUsersImage() throws Exception {
		this.mockMvc.perform(uploadAs("team-photo", PNG, false, TestUsers.signedInWithId("a@example.com", 101))).andExpect(status().isCreated());
		this.mockMvc.perform(delete(MEDIA + "/team-photo").with(TestUsers.signedInWithId("b@example.com", 102)).with(csrf()))
			.andExpect(status().isForbidden());
		this.mockMvc.perform(get(MEDIA + "/team-photo")).andExpect(content().bytes(PNG));
	}

	@Test
	void answers404ForAnUnknownSlugAndRefusesTheUnauthenticatedOrTokenless() throws Exception {
		this.mockMvc.perform(delete(MEDIA + "/nothing").with(TestUsers.signedIn()).with(csrf())).andExpect(status().isNotFound());
		this.mockMvc.perform(uploadAs("team-photo", PNG, false, TestUsers.signedIn())).andExpect(status().isCreated());
		this.mockMvc.perform(delete(MEDIA + "/team-photo").with(csrf())).andExpect(status().isUnauthorized());
		this.mockMvc.perform(delete(MEDIA + "/team-photo").with(TestUsers.signedIn())).andExpect(status().isForbidden());
		this.mockMvc.perform(get(MEDIA + "/team-photo")).andExpect(content().bytes(PNG));
	}

}
