package com.simpleblog.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.simpleblog.entity.BlogUser;
import com.simpleblog.repository.UserRepository;
import com.simpleblog.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class AllowedOidcUserServiceTests {

	private final AllowedUsers allowed = new AllowedUsers(new String[] { " Owner@Example.com ", "" });

	private final Map<String, BlogUser> users = new HashMap<>();

	private final UserService userService = new UserService(new UserRepository() {

		@Override
		public BlogUser save(BlogUser user) {
			AllowedOidcUserServiceTests.this.users.put(user.email(), user);
			return user;
		}

		@Override
		public Optional<BlogUser> findByEmail(String email) {
			return Optional.ofNullable(AllowedOidcUserServiceTests.this.users.get(email));
		}

	}, Clock.systemUTC());

	@Test
	void allowedUsersAreMatchedIgnoringCase() {
		assertThat(this.allowed.permits("owner@example.com")).isTrue();
		assertThat(this.allowed.permits("OWNER@EXAMPLE.COM")).isTrue();
		assertThat(this.allowed.permits("other@example.com")).isFalse();
		assertThat(this.allowed.permits(null)).isFalse();
	}

	@Test
	void everyoneIsAllowedWhenTheListIsEmpty() {
		assertThat(new AllowedUsers(new String[0]).permits("anyone@example.com")).isTrue();
		assertThat(new AllowedUsers(new String[] { "" }).permits("anyone@example.com")).isTrue();
		assertThat(new AllowedUsers(new String[0]).permits(null)).isFalse();
		assertThat(new AllowedUsers(new String[0]).permits(" ")).isFalse();
	}

	@Test
	void anyVerifiedUserSignsInAndIsRegisteredWhenTheListIsEmpty() {
		OidcUser user = userOf("anyone@example.com", true);
		AllowedOidcUserService open = new AllowedOidcUserService(new AllowedUsers(new String[0]), this.userService, (request) -> user);
		assertThat(open.loadUser(request())).isSameAs(user);
		assertThat(this.users).containsOnlyKeys("anyone@example.com");
	}

	@Test
	void anUnverifiedEmailIsStillRefusedWhenTheListIsEmpty() {
		OidcUser user = userOf("anyone@example.com", false);
		AllowedOidcUserService open = new AllowedOidcUserService(new AllowedUsers(new String[0]), this.userService, (request) -> user);
		assertThatThrownBy(() -> open.loadUser(request())).isInstanceOf(OAuth2AuthenticationException.class);
		assertThat(this.users).isEmpty();
	}

	@Test
	void anAllowedVerifiedUserSignsIn() {
		OidcUser user = userOf("owner@example.com", true);
		assertThat(serviceReturning(user).loadUser(request())).isSameAs(user);
	}

	@Test
	void anAllowedUserIsRegisteredByEmailAndProviderWhenNew() {
		serviceReturning(userOf("owner@example.com", true)).loadUser(request());
		assertThat(this.users).containsOnlyKeys("owner@example.com");
		assertThat(this.users.get("owner@example.com").provider()).isEqualTo("google");
	}

	@Test
	void aRejectedUserIsNotRegistered() {
		assertUnauthorized(userOf("other@example.com", true));
		assertThat(this.users).isEmpty();
	}

	@Test
	void anotherUserIsUnauthorized() {
		assertUnauthorized(userOf("other@example.com", true));
	}

	@Test
	void anUnverifiedEmailIsUnauthorizedEvenWhenListed() {
		assertUnauthorized(userOf("owner@example.com", false));
	}

	private void assertUnauthorized(OidcUser user) {
		assertThatThrownBy(() -> serviceReturning(user).loadUser(request())).isInstanceOf(OAuth2AuthenticationException.class)
			.satisfies((error) -> assertThat(((OAuth2AuthenticationException) error).getError().getErrorCode())
				.isEqualTo(AllowedOidcUserService.UNAUTHORIZED_USER))
			.hasMessageContaining("Unauthorized user");
	}

	private AllowedOidcUserService serviceReturning(OidcUser user) {
		return new AllowedOidcUserService(this.allowed, this.userService, (request) -> user);
	}

	private OidcUserRequest request() {
		ClientRegistration google = ClientRegistration.withRegistrationId("google").clientId("c")
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri("http://localhost/cb")
			.authorizationUri("http://localhost/a").tokenUri("http://localhost/t").build();
		Instant now = Instant.now();
		OAuth2AccessToken access = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "t", now, now.plusSeconds(60));
		return new OidcUserRequest(google, access, new OidcIdToken("t", now, now.plusSeconds(60), Map.of("sub", "1")));
	}

	private OidcUser userOf(String email, boolean verified) {
		Instant now = Instant.now();
		OidcIdToken token = new OidcIdToken("t", now, now.plusSeconds(60),
				Map.of("sub", "1", "email", email, "email_verified", verified));
		return new DefaultOidcUser(java.util.List.of(), token);
	}

}
