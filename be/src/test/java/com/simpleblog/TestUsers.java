package com.simpleblog;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.simpleblog.component.BlogOidcUser;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;

/** Signed-in Google users for the MockMvc tests. */
final class TestUsers {

	static final String OWNER = "owner@example.com";

	private TestUsers() {
	}

	static OidcLoginRequestPostProcessor signedIn() {
		return signedIn(OWNER);
	}

	/** A session that already holds the user id, as one made by signing in does; the id is not looked up by email. */
	static OidcLoginRequestPostProcessor signedInWithId(String email, long userId) {
		Instant now = Instant.now();
		OidcIdToken token = new OidcIdToken("t", now, now.plusSeconds(60), Map.of("sub", "1", "email", email));
		return oidcLogin().oidcUser(new BlogOidcUser(new DefaultOidcUser(List.of(), token), userId));
	}

	static OidcLoginRequestPostProcessor signedIn(String email) {
		return oidcLogin().idToken((token) -> token.claim("email", email));
	}

}
