package com.simpleblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.simpleblog.component.LoginReturn;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The Google sign-in with a client configured. It checks the server's side of the flow without calling Google: the
 * authorization request it sends the browser to, how a failed or declined callback ends, and the identity endpoint.
 */
@SpringBootTest(properties = { "simpleblog.auth.google.client-id=test-client-id",
		"simpleblog.auth.google.client-secret=test-client-secret", "simpleblog.auth.frontend-url=http://frontend.test/",
		"simpleblog.auth.allowed-emails=owner@example.com" })
@AutoConfigureMockMvc
class GoogleLoginTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void startRedirectsToGoogleWithTheOpenIdConnectRequest() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/public/auth/google")).andExpect(status().isFound()).andReturn();
		String location = result.getResponse().getHeader("Location");
		var uri = UriComponentsBuilder.fromUriString(location).build();
		assertThat(location).startsWith("https://accounts.google.com/o/oauth2/v2/auth?");
		assertThat(uri.getQueryParams().getFirst("client_id")).isEqualTo("test-client-id");
		assertThat(uri.getQueryParams().getFirst("response_type")).isEqualTo("code");
		assertThat(uri.getQueryParams().getFirst("redirect_uri")).isEqualTo("http://localhost/public/auth/google/callback");
		assertThat(uri.getQueryParams().getFirst("scope")).isEqualTo("openid%20profile%20email");
		assertThat(uri.getQueryParams().getFirst("state")).isNotBlank();
		assertThat(uri.getQueryParams().getFirst("nonce")).isNotBlank();
	}

	@Test
	void startKeepsThePageToComeBackTo() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/public/auth/google").param("returnTo", "/post/hello")).andExpect(status().isFound()).andReturn();
		assertThat(result.getRequest().getSession().getAttribute(LoginReturn.ATTRIBUTE)).isEqualTo("/post/hello");
	}

	@Test
	void theClientSecretNeverReachesTheBrowser() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/public/auth/google")).andReturn();
		assertThat(result.getResponse().getHeader("Location")).doesNotContain("test-client-secret");
		assertThat(result.getResponse().getContentAsString()).doesNotContain("test-client-secret");
	}

	@Test
	void aCallbackWithAnUnknownStateFailsBackToTheFrontendWithoutASession() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/public/auth/google/callback").param("code", "abc").param("state", "forged"))
			.andExpect(status().isFound())
			.andReturn();
		assertThat(result.getResponse().getHeader("Location")).isEqualTo("http://frontend.test/?login=failed");
		assertThat(result.getRequest().getSession(false)).isNull();
	}

	@Test
	void aDeclinedConsentFailsBackToTheFrontend() throws Exception {
		MvcResult result = this.mockMvc
			.perform(get("/public/auth/google/callback").param("error", "access_denied").param("state", "forged"))
			.andExpect(status().isFound())
			.andReturn();
		assertThat(result.getResponse().getHeader("Location")).isEqualTo("http://frontend.test/?login=failed");
	}

	@Test
	void meIsUnauthorizedWithoutASessionAndStartsNone() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/public/api/v1/me")).andExpect(status().isUnauthorized()).andReturn();
		assertThat(result.getRequest().getSession(false)).isNull();
		assertThat(result.getResponse().getHeaders("Set-Cookie")).noneMatch((cookie) -> cookie.startsWith("JSESSIONID"));
	}

	@Test
	void meShowsTheEmailOfTheSignedInUser() throws Exception {
		this.mockMvc
			.perform(get("/public/api/v1/me")
				.with(oidcLogin().idToken((token) -> token.claim("email", "owner@example.com"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("owner@example.com"));
	}

}
