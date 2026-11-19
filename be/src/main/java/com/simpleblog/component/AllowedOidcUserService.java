package com.simpleblog.component;

import com.simpleblog.entity.BlogUser;
import com.simpleblog.service.UserService;
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
 * sign-in without creating a session. Whoever is let in is looked up by email in TB_USER and registered with a new id
 * when absent, so what they create can name them by id. The id travels in the session as a {@link BlogOidcUser}.
 */
@Component
public class AllowedOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

	public static final String UNAUTHORIZED_USER = "unauthorized_user";

	private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

	private final AllowedUsers allowedUsers;

	private final UserService userService;

	@Autowired
	public AllowedOidcUserService(AllowedUsers allowedUsers, UserService userService) {
		this(allowedUsers, userService, new OidcUserService());
	}

	AllowedOidcUserService(AllowedUsers allowedUsers, UserService userService,
			OAuth2UserService<OidcUserRequest, OidcUser> delegate) {
		this.allowedUsers = allowedUsers;
		this.userService = userService;
		this.delegate = delegate;
	}

	@Override
	public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
		OidcUser user = this.delegate.loadUser(request);
		if (!Boolean.TRUE.equals(user.getEmailVerified()) || !this.allowedUsers.permits(user.getEmail())) {
			throw new OAuth2AuthenticationException(new OAuth2Error(UNAUTHORIZED_USER, "Unauthorized user: " + user.getEmail(), null));
		}
		BlogUser registered = this.userService.registration(user.getEmail(), request.getClientRegistration().getRegistrationId());
		return new BlogOidcUser(user, registered.id());
	}

}
