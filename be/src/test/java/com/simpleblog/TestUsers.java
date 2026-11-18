package com.simpleblog;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;

/** Signed-in Google users for the MockMvc tests. */
final class TestUsers {

	static final String OWNER = "owner@example.com";

	private TestUsers() {
	}

	static OidcLoginRequestPostProcessor signedIn() {
		return signedIn(OWNER);
	}

	static OidcLoginRequestPostProcessor signedIn(String email) {
		return oidcLogin().idToken((token) -> token.claim("email", email));
	}

}
