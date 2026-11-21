package com.simpleblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * POST /public/api/v1/post and PUT /public/api/v1/post/{slug} against H2, with the write guard on (see the test
 * config) and a fixed clock: 2026-11-17 01:30:00.987654 UTC is 10:30:00 in Asia/Seoul, and times are cut to whole
 * seconds. Fixture IDs start at 1001 so they never meet the IDs the database generates. Each test runs in a
 * transaction that is rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql(statements = """
		insert into tb_user (ID, EMAIL, PROVIDER, REGISTERED_DATE) values
		(1, 'owner@example.com', 'google', now()),
		(2, 'other@example.com', 'google', now());
		insert into tb_post (ID, TITLE, CONTENT, SLUG, TYPE, STATUS, AUTHOR_ID, CREATED_DATETIME, MODIFIED_DATETIME) values
		(1001, 'KPI', '<p>old</p>', 'kpi', 'post', 'publish', 1, '2020-01-01 00:00:00', '2020-01-02 00:00:00'),
		(1002, 'A draft', 'draft body', 'a-draft', 'post', 'draft', 1, '2021-01-01 00:00:00', '2021-01-01 00:00:00'),
		(1003, 'About', 'page body', 'about', 'page', 'publish', 1, '2022-01-01 00:00:00', '2022-01-01 00:00:00'),
		(1004, 'Image', '', 'hi-there', 'attachment', 'inherit', 1, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
		(1005, 'Digits', 'old', 'post_2026', 'post', 'publish', 1, '2024-01-01 00:00:00', '2024-01-01 00:00:00'),
		(1006, 'Korean', 'old', '%ed%81%b0-kpi', 'post', 'draft', 1, '2024-01-01 00:00:00', '2024-01-01 00:00:00');
		""")
class PostWriteControllerTests {

	private static final String POST = "/public/api/v1/post";

	@TestConfiguration
	static class FixedClock {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(Instant.parse("2026-11-17T01:30:00.987654Z"), ZoneId.of("Asia/Seoul"));
		}

	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	private static String json(String title, String slug, String content) {
		return """
				{"title": %s, "slug": %s, "content": %s}""".formatted(quoted(title), quoted(slug), quoted(content));
	}

	private static String jsonWithStatus(String title, String slug, String content, String status) {
		return """
				{"title": %s, "slug": %s, "content": %s, "status": %s}""".formatted(quoted(title), quoted(slug), quoted(content),
				quoted(status));
	}

	private static String quoted(String value) {
		return value == null ? "null" : "\"" + value.replace("\"", "\\\"") + "\"";
	}

	private MockHttpServletRequestBuilder created(String body) {
		return post(POST).with(TestUsers.signedIn()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private MockHttpServletRequestBuilder updated(String slug, String body) {
		return put(POST + "/{slug}", slug).with(TestUsers.signedIn()).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body);
	}

	private Map<String, Object> row(String storedSlug) {
		this.entityManager.flush();
		return this.jdbcTemplate.queryForMap("select * from tb_post where slug = ?", storedSlug);
	}

	@Test
	void createsAPublishedPostWhenTheStatusIsPublish() throws Exception {
		this.mockMvc.perform(created(jsonWithStatus("Out now", "out-now", "body", "publish")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("publish"));
	}

	@Test
	void publishesADraftByUpdatingItWithTheStatusPublish() throws Exception {
		this.mockMvc.perform(updated("a-draft", jsonWithStatus("A draft", "a-draft", "body", "publish")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("publish"));
	}

	@Test
	void anUpdateWithoutAStatusKeepsTheStatus() throws Exception {
		this.mockMvc.perform(updated("a-draft", json("A draft", "a-draft", "body")))
			.andExpect(jsonPath("$.status").value("draft"));
	}

	@Test
	void savesTheHashTagsOfANewPostAndKeepsThemWhenAnUpdateLeavesThemOut() throws Exception {
		String body = "{\"title\": \"Tagged\", \"slug\": \"tagged\", \"content\": \"x\", \"hashTags\": \"경영,KPI\"}";
		this.mockMvc.perform(created(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.hashTags").value("경영,KPI"));
		this.mockMvc.perform(updated("tagged", json("Tagged", "tagged", "y"))).andExpect(jsonPath("$.hashTags").value("경영,KPI"));
		org.assertj.core.api.Assertions.assertThat(row("tagged")).containsEntry("HASH_TAGS", "경영,KPI");
	}

	@Test
	void replacesOrClearsTheHashTagsOnUpdate() throws Exception {
		this.mockMvc.perform(updated("kpi", "{\"title\": \"KPI\", \"slug\": \"kpi\", \"content\": \"x\", \"hashTags\": \"a,b\"}"))
			.andExpect(jsonPath("$.hashTags").value("a,b"));
		this.mockMvc.perform(updated("kpi", "{\"title\": \"KPI\", \"slug\": \"kpi\", \"content\": \"x\", \"hashTags\": \"\"}"))
			.andExpect(jsonPath("$.hashTags").value(""));
	}

	@Test
	void refusesAStatusOtherThanDraftOrPublish() throws Exception {
		this.mockMvc.perform(created(jsonWithStatus("Odd", "odd", "body", "private"))).andExpect(status().isBadRequest());
		this.mockMvc.perform(updated("kpi", jsonWithStatus("KPI", "kpi", "body", "trash"))).andExpect(status().isBadRequest());
	}

	@Test
	void createsADraftPostAndReturnsItsStoredValues() throws Exception {
		this.mockMvc.perform(created(json("New thought", "new-thought", "<p>hi</p>")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.title").value("New thought"))
			.andExpect(jsonPath("$.slug").value("new-thought"))
			.andExpect(jsonPath("$.content").value("<p>hi</p>"))
			.andExpect(jsonPath("$.status").value("draft"))
			.andExpect(jsonPath("$.type").value("post"))
			.andExpect(jsonPath("$.date").value("2026-11-17T10:30:00"))
			.andExpect(jsonPath("$.modified").value("2026-11-17T10:30:00"))
			.andExpect(jsonPath("$.canEdit").value(true));
		Map<String, Object> row = row("new-thought");
		org.assertj.core.api.Assertions.assertThat(row)
			.containsEntry("TYPE", "post")
			.containsEntry("STATUS", "draft")
			.containsEntry("AUTHOR_ID", this.jdbcTemplate.queryForObject("select id from tb_user where email = ?", Long.class, TestUsers.OWNER));
		org.assertj.core.api.Assertions.assertThat(row.get("CREATED_DATETIME").toString()).startsWith("2026-11-17 01:30:00");
	}

	@Test
	void acceptsEmptyContent() throws Exception {
		this.mockMvc.perform(created(json("Title", "empty-content", ""))).andExpect(status().isCreated());
	}

	@ParameterizedTest
	@ValueSource(strings = { "kpi", "about", "hi-there" })
	void refusesASlugAnyRowAlreadyHasWithConflict(String slug) throws Exception {
		this.mockMvc.perform(created(json("Title", slug, "body"))).andExpect(status().isConflict());
	}

	@Test
	void refusesInputThatBreaksTheSpecWithBadRequest() throws Exception {
		for (String body : new String[] { json(null, "s", "c"), json("  ", "s", "c"), json("t", null, "c"),
				json("t", "   ", "c"), json("t", "s", null), json("t", "a".repeat(201), "c"), "{", "" }) {
			this.mockMvc.perform(created(body)).andExpect(status().isBadRequest());
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "has space", " lead", "trail ", "큰기업", "under_score", "a/b", "a?b", "100%" })
	void refusesASlugWithAnythingButEnglishLettersAndHyphen(String slug) throws Exception {
		this.mockMvc.perform(created(json("Title", slug, "body"))).andExpect(status().isBadRequest());
		this.mockMvc.perform(updated("kpi", json("Title", slug, "body"))).andExpect(status().isBadRequest());
	}

	@Test
	void limitsTheTitleTo130CharactersAndTheSlugTo200() throws Exception {
		this.mockMvc.perform(created(json("가".repeat(131), "too-long-title", "c"))).andExpect(status().isBadRequest());
		this.mockMvc.perform(created(json("가".repeat(130), "max-title", "c"))).andExpect(status().isCreated());
		this.mockMvc.perform(created(json("t", "a".repeat(201), "c"))).andExpect(status().isBadRequest());
		this.mockMvc.perform(created(json("t", "a".repeat(200), "c"))).andExpect(status().isCreated());
	}

	@Test
	void acceptsEnglishLettersAndHyphenInAnyCase() throws Exception {
		this.mockMvc.perform(created(json("Title", "Mixed-Case-slug", "body"))).andExpect(status().isCreated());
	}

	@Test
	void updatesTitleAndContentAndKeepsTheStatusAndDate() throws Exception {
		this.mockMvc.perform(updated("kpi", json("KPI 2", "kpi", "<p>new</p>")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("KPI 2"))
			.andExpect(jsonPath("$.content").value("<p>new</p>"))
			.andExpect(jsonPath("$.status").value("publish"))
			.andExpect(jsonPath("$.date").value("2020-01-01T09:00:00"))
			.andExpect(jsonPath("$.modified").value("2026-11-17T10:30:00"))
			.andExpect(jsonPath("$.canEdit").value(true));
		org.assertj.core.api.Assertions.assertThat(row("kpi").get("MODIFIED_DATETIME").toString())
			.startsWith("2026-11-17 01:30:00");
	}

	@Test
	void refusesUpdatingAPostWrittenByAnotherUser() throws Exception {
		this.mockMvc.perform(updated("kpi", json("Hijacked", "kpi", "x")).with(TestUsers.signedIn("other@example.com")))
			.andExpect(status().isForbidden());
		org.assertj.core.api.Assertions.assertThat(row("kpi")).containsEntry("TITLE", "KPI");
	}

	@Test
	void refusesUpdatingAPostForASignedInUserWithNoRegistration() throws Exception {
		this.mockMvc.perform(updated("kpi", json("Hijacked", "kpi", "x")).with(TestUsers.signedIn("new@example.com")))
			.andExpect(status().isForbidden());
	}

	@Test
	void updatesADraftWithoutPublishingIt() throws Exception {
		this.mockMvc.perform(updated("a-draft", json("Still a draft", "a-draft", "x")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("draft"));
	}

	@Test
	void renamesAPostSoTheOldSlugNoLongerResolves() throws Exception {
		this.mockMvc.perform(updated("kpi", json("KPI", "kpi-renamed", "x")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.slug").value("kpi-renamed"));
		this.mockMvc.perform(get(POST + "/kpi")).andExpect(status().isNotFound());
		this.mockMvc.perform(get(POST + "/kpi-renamed")).andExpect(status().isOk());
	}

	@Test
	void refusesRenamingOntoAnotherRowsSlugWithConflict() throws Exception {
		this.mockMvc.perform(updated("kpi", json("KPI", "a-draft", "x"))).andExpect(status().isConflict());
		this.mockMvc.perform(updated("kpi", json("KPI", "about", "x"))).andExpect(status().isConflict());
	}

	@Test
	void answersNotFoundForAnUnknownSlugAndForRowsThatAreNeitherPostsNorPages() throws Exception {
		for (String slug : new String[] { "no-such-post", "hi-there" }) {
			this.mockMvc.perform(updated(slug, json("T", "t-" + slug, "x"))).andExpect(status().isNotFound());
		}
	}

	@Test
	void keepsAnUnchangedSlugThatBreaksTheSlugRule() throws Exception {
		this.mockMvc.perform(updated("post_2026", json("Digits 2", "post_2026", "new")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.slug").value("post_2026"))
			.andExpect(jsonPath("$.title").value("Digits 2"));
		this.mockMvc.perform(updated("큰-kpi", jsonWithStatus("Korean", "큰-kpi", "new", "publish")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.slug").value("%ed%81%b0-kpi"))
			.andExpect(jsonPath("$.status").value("publish"));
	}

	@Test
	void stillAppliesTheSlugRuleToANewSlugAndToTheRestOfTheInput() throws Exception {
		this.mockMvc.perform(updated("post_2026", json("Digits", "post_2027", "x"))).andExpect(status().isBadRequest());
		this.mockMvc.perform(updated("post_2026", json("", "post_2026", "x"))).andExpect(status().isBadRequest());
		this.mockMvc.perform(updated("post_2026", json("Digits", "post_2026", null))).andExpect(status().isBadRequest());
		this.mockMvc.perform(created(json("Underscore", "post_2026", "x"))).andExpect(status().isBadRequest());
	}

	@Test
	void acceptsDigitsInASlugWhenCreatingAndRenaming() throws Exception {
		this.mockMvc.perform(created(json("Year", "kpi-2026", "x"))).andExpect(status().isCreated())
			.andExpect(jsonPath("$.slug").value("kpi-2026"));
		this.mockMvc.perform(updated("kpi", json("KPI", "kpi-2", "x"))).andExpect(status().isOk());
	}

	@Test
	void updatesAPageForItsAuthorAndRefusesAnotherUser() throws Exception {
		this.mockMvc.perform(updated("about", json("About us", "about", "<p>new</p>")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("About us"))
			.andExpect(jsonPath("$.type").value("page"))
			.andExpect(jsonPath("$.canEdit").value(true));
		this.mockMvc.perform(updated("about", json("Hijacked", "about", "x")).with(TestUsers.signedIn("other@example.com")))
			.andExpect(status().isForbidden());
		org.assertj.core.api.Assertions.assertThat(row("about")).containsEntry("TYPE", "page").containsEntry("TITLE", "About us");
	}

	@Test
	void refusesAnUpdateThatBreaksTheSpecWithBadRequest() throws Exception {
		this.mockMvc.perform(updated("kpi", json("", "kpi", "x"))).andExpect(status().isBadRequest());
		this.mockMvc.perform(updated("kpi", json("T", "kpi", null))).andExpect(status().isBadRequest());
	}

	private MockHttpServletRequestBuilder deleted(String slug) {
		return delete(POST + "/{slug}", slug).with(TestUsers.signedIn()).with(csrf());
	}

	private int rowsWithSlug(String slug) {
		this.entityManager.flush();
		return this.jdbcTemplate.queryForObject("select count(*) from tb_post where slug = ?", Integer.class, slug);
	}

	@Test
	void deletesAPostWhateverItsStatus() throws Exception {
		this.mockMvc.perform(deleted("kpi")).andExpect(status().isNoContent());
		this.mockMvc.perform(deleted("a-draft")).andExpect(status().isNoContent());
		assertThat(rowsWithSlug("kpi") + rowsWithSlug("a-draft")).isZero();
		this.mockMvc.perform(get(POST + "/kpi")).andExpect(status().isNotFound());
	}

	@Test
	void deletesOnlyThePostWithThatSlug() throws Exception {
		this.mockMvc.perform(deleted("kpi")).andExpect(status().isNoContent());
		assertThat(rowsWithSlug("a-draft") + rowsWithSlug("about") + rowsWithSlug("hi-there")).isEqualTo(3);
	}

	@Test
	void refusesDeletingAPostWrittenByAnotherUser() throws Exception {
		this.mockMvc.perform(delete(POST + "/kpi").with(TestUsers.signedIn("other@example.com")).with(csrf()))
			.andExpect(status().isForbidden());
		assertThat(rowsWithSlug("kpi")).isEqualTo(1);
	}

	@Test
	void answersNotFoundWhenDeletingAnUnknownSlugOrARowThatIsNeitherAPostNorAPage() throws Exception {
		for (String slug : new String[] { "no-such-post", "hi-there" }) {
			this.mockMvc.perform(deleted(slug)).andExpect(status().isNotFound());
		}
		assertThat(rowsWithSlug("hi-there")).isEqualTo(1);
	}

	@Test
	void deletesAPageForItsAuthorAndRefusesAnotherUser() throws Exception {
		this.mockMvc.perform(delete(POST + "/about").with(TestUsers.signedIn("other@example.com")).with(csrf()))
			.andExpect(status().isForbidden());
		assertThat(rowsWithSlug("about")).isEqualTo(1);
		this.mockMvc.perform(deleted("about")).andExpect(status().isNoContent());
		assertThat(rowsWithSlug("about")).isZero();
	}

	@Test
	void refusesADeleteWithoutASessionOrACsrfToken() throws Exception {
		this.mockMvc.perform(delete(POST + "/kpi").with(csrf())).andExpect(status().isUnauthorized());
		this.mockMvc.perform(delete(POST + "/kpi").with(TestUsers.signedIn())).andExpect(status().isForbidden());
		assertThat(rowsWithSlug("kpi")).isEqualTo(1);
	}

	@Test
	void requiresASignedInSession() throws Exception {
		this.mockMvc.perform(post(POST).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json("T", "unauth", "x")))
			.andExpect(status().isUnauthorized());
		this.mockMvc.perform(put(POST + "/kpi").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json("T", "kpi", "x")))
			.andExpect(status().isUnauthorized());
		assertThat(this.jdbcTemplate.queryForObject("select count(*) from tb_post where slug = 'unauth'", Integer.class))
			.isZero();
	}

	@Test
	void requiresACsrfToken() throws Exception {
		this.mockMvc.perform(post(POST).with(TestUsers.signedIn()).contentType(MediaType.APPLICATION_JSON).content(json("T", "nocsrf", "x")))
			.andExpect(status().isForbidden());
		this.mockMvc.perform(put(POST + "/kpi").with(TestUsers.signedIn()).contentType(MediaType.APPLICATION_JSON).content(json("T", "kpi", "x")))
			.andExpect(status().isForbidden());
	}

	@Test
	void keepsReadsPublic() throws Exception {
		this.mockMvc.perform(get(POST + "/kpi")).andExpect(status().isOk()).andExpect(jsonPath("$.slug").value("kpi"));
	}

}
