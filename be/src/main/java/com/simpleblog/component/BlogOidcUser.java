package com.simpleblog.component;

import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * The Google user of a signed-in session together with the user's id in TB_USER, found or registered when signing in.
 * It lives in the session with the rest of the authentication, so later requests know the id without asking the database.
 */
public class BlogOidcUser extends DefaultOidcUser {

	private static final long serialVersionUID = 1L;

	private final long userId;

	public BlogOidcUser(OidcUser user, long userId) {
		super(user.getAuthorities(), user.getIdToken(), user.getUserInfo());
		this.userId = userId;
	}

	/** The id of TB_USER. */
	public long userId() {
		return this.userId;
	}

}
