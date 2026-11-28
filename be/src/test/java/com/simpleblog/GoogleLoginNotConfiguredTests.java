package com.simpleblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Without a Google client configured, which is the default, the app runs and just offers no sign-in. */
// The empty client id states what the test is about, and gives it a context of its own: the csrf() of a write test swaps the CSRF token
// repository of the context it runs in for one that keeps the token in a session, which this test must not see.
@SpringBootTest(properties = "simpleblog.auth.google.client-id=")
@AutoConfigureMockMvc
class GoogleLoginNotConfiguredTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void theSignInIsNotAvailable() throws Exception {
		this.mockMvc.perform(get("/public/auth/google")).andExpect(status().isNotFound());
	}

	@Test
	void meIsUnauthorized() throws Exception {
		this.mockMvc.perform(get("/public/api/v1/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void anonymousReadsStartNoSession() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/public/api/v1/posts")).andExpect(status().isOk()).andReturn();
		assertThat(result.getRequest().getSession(false)).isNull();
	}

}
