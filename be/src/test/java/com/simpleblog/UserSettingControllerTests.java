package com.simpleblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** GET and PUT /public/api/v1/user and PUT /public/api/v1/user/access-token against H2. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserSettingControllerTests {

	private static final String USER = "/public/api/v1/user";

	private static final String BODY = """
			{"nick": " Chi ", "mainTheme": "My Blog", "subTheme": "", "aboutSlug": "me", "workingSlug": "work-1"}""";

	@Autowired
	private MockMvc mockMvc;

	private static RequestPostProcessor owner() {
		return TestUsers.signedInWithId("a@example.com", 101);
	}

	private MockHttpServletRequestBuilder update(String body, RequestPostProcessor user) {
		return put(USER).with(user).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	@Test
	void answersTheDefaultsBeforeAnythingIsSaved() throws Exception {
		this.mockMvc.perform(get(USER).with(owner()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("a@example.com"))
			.andExpect(jsonPath("$.nick").value(org.hamcrest.Matchers.nullValue()))
			.andExpect(jsonPath("$.mainTheme").value("Dreaming for the Future"))
			.andExpect(jsonPath("$.subTheme").value("영원한 개발자를 향해"))
			.andExpect(jsonPath("$.aboutName").value("나의 꿈"))
			.andExpect(jsonPath("$.aboutSlug").value("about"))
			.andExpect(jsonPath("$.workingName").value("제가 일하는 곳은?"))
			.andExpect(jsonPath("$.workingSlug").value("my-working-places"))
			.andExpect(jsonPath("$.mcpAccessKey").value(org.hamcrest.Matchers.nullValue()))
			.andExpect(jsonPath("$.modifiedDate").value(org.hamcrest.Matchers.nullValue()));
	}

	@Test
	void savesTheEditableFieldsAndReadsThemBack() throws Exception {
		this.mockMvc.perform(update(BODY, owner()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nick").value("Chi"))
			.andExpect(jsonPath("$.mainTheme").value("My Blog"))
			.andExpect(jsonPath("$.subTheme").value(""))
			.andExpect(jsonPath("$.aboutSlug").value("me"))
			.andExpect(jsonPath("$.workingSlug").value("work-1"))
			.andExpect(jsonPath("$.aboutName").value("나의 꿈"));
		this.mockMvc.perform(get(USER).with(owner()))
			.andExpect(jsonPath("$.workingSlug").value("work-1"))
			.andExpect(jsonPath("$.modifiedDate").isNotEmpty());
	}

	@Test
	void clearsTheNickWhenItIsBlankAndIgnoresFieldsItMayNotChange() throws Exception {
		this.mockMvc.perform(update(BODY, owner())).andExpect(status().isOk());
		String body = """
				{"nick": "  ", "mainTheme": "My Blog", "subTheme": "s", "aboutSlug": "me", "workingSlug": "w",
				 "aboutName": "Changed", "mcpAccessKey": "forged"}""";
		this.mockMvc.perform(update(body, owner()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nick").value(org.hamcrest.Matchers.nullValue()))
			.andExpect(jsonPath("$.aboutName").value("나의 꿈"))
			.andExpect(jsonPath("$.mcpAccessKey").value(org.hamcrest.Matchers.nullValue()));
	}

	@Test
	void refusesWhatCannotBeStored() throws Exception {
		String longNick = "n".repeat(61);
		for (String body : new String[] {
				BODY.replace("Chi", longNick), BODY.replace("My Blog", " "), BODY.replace("My Blog", "t".repeat(257)),
				BODY.replace("\"me\"", "\"has space\""), BODY.replace("\"me\"", "\"한글\""), BODY.replace("work-1", "w".repeat(201)),
				BODY.replace("\"work-1\"", "null"), BODY.replace("\"subTheme\": \"\"", "\"subTheme\": \"" + "s".repeat(257) + "\"") }) {
			this.mockMvc.perform(update(body, owner())).andExpect(status().isBadRequest());
		}
	}

	@Test
	void renewsTheAccessKeyEachTimeAndKeepsItOutOfTheUpdate() throws Exception {
		String first = this.mockMvc.perform(put(USER + "/access-token").with(owner()).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.mcpAccessKey").isNotEmpty())
			.andReturn().getResponse().getContentAsString();
		String second = this.mockMvc.perform(put(USER + "/access-token").with(owner()).with(csrf()))
			.andReturn().getResponse().getContentAsString();
		String key = JsonPath.read(second, "$.mcpAccessKey");
		assertThat(key).isNotEqualTo(JsonPath.<String>read(first, "$.mcpAccessKey")).hasSizeGreaterThanOrEqualTo(40);
		this.mockMvc.perform(update(BODY, owner())).andExpect(jsonPath("$.mcpAccessKey").value(key));
		this.mockMvc.perform(get(USER).with(owner())).andExpect(jsonPath("$.mcpAccessKey").value(key));
	}

	@Test
	void keepsEachUsersSettingsApart() throws Exception {
		this.mockMvc.perform(update(BODY, owner())).andExpect(status().isOk());
		this.mockMvc.perform(get(USER).with(TestUsers.signedInWithId("b@example.com", 102)))
			.andExpect(jsonPath("$.mainTheme").value("Dreaming for the Future"))
			.andExpect(jsonPath("$.email").value("b@example.com"));
	}

	@Test
	void needsASessionAndForAWriteACsrfToken() throws Exception {
		this.mockMvc.perform(get(USER)).andExpect(status().isUnauthorized());
		this.mockMvc.perform(put(USER).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
		this.mockMvc.perform(put(USER + "/access-token").with(csrf())).andExpect(status().isUnauthorized());
		this.mockMvc.perform(put(USER).with(owner()).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
		this.mockMvc.perform(put(USER + "/access-token").with(owner())).andExpect(status().isForbidden());
	}

}
