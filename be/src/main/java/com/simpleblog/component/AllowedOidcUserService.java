package com.simpleblog.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Loads the Google user as usual, then turns away anyone whose verified email is not an allowed user by throwing an
 * {@link OAuth2AuthenticationException} with the error code {@link #UNAUTHORIZED_USER}. Spring Security then ends the
 * sign-in without creating a session.
 */
@Component
public class AllowedOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

	public static final String UNAUTHORIZED_USER = "unauthorized_user";

	private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

	private final AllowedUsers allowedUsers;

	@Autowired
	public AllowedOidcUserService(AllowedUsers allowedUsers) {
		this(allowedUsers, new OidcUserService());
	}

	AllowedOidcUserService(AllowedUsers allowedUsers, OAuth2UserService<OidcUserRequest, OidcUser> delegate) {
		this.allowedUsers = allowedUsers;
		this.delegate = delegate;
	}

	@Override
	public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
		OidcUser user = this.delegate.loadUser(request);
		if (!Boolean.TRUE.equals(user.getEmailVerified()) || !this.allowedUsers.permits(user.getEmail())) {
			throw new OAuth2AuthenticationException(new OAuth2Error(UNAUTHORIZED_USER, "Unauthorized user: " + user.getEmail(), null));
		}
		return user;
	}

}
