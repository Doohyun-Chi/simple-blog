package com.simpleblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/** POST /public/auth/logout ends the session of the caller. */
@SpringBootTest
@AutoConfigureMockMvc
class LogoutTests {

	private static final String LOGOUT = "/public/auth/logout";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void invalidatesTheSessionAndAnswers204() throws Exception {
		MockHttpSession session = new MockHttpSession();
		this.mockMvc.perform(post(LOGOUT).session(session).with(TestUsers.signedIn()).with(csrf())).andExpect(status().isNoContent());
		assertThat(session.isInvalid()).isTrue();
	}

	@Test
	void answers204AlsoWhenNobodyIsSignedIn() throws Exception {
		this.mockMvc.perform(post(LOGOUT).with(csrf())).andExpect(status().isNoContent());
	}

	@Test
	void keepsTheSessionWhenTheRequestHasNoCsrfToken() throws Exception {
		MockHttpSession session = new MockHttpSession();
		this.mockMvc.perform(post(LOGOUT).session(session).with(TestUsers.signedIn())).andExpect(status().isForbidden());
		assertThat(session.isInvalid()).isFalse();
	}

}
