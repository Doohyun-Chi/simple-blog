package com.simpleblog.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class AllowedOidcUserServiceTests {

	private final AllowedUsers allowed = new AllowedUsers(new String[] { " Owner@Example.com ", "" });

	@Test
	void allowedUsersAreMatchedIgnoringCase() {
		assertThat(this.allowed.permits("owner@example.com")).isTrue();
		assertThat(this.allowed.permits("OWNER@EXAMPLE.COM")).isTrue();
		assertThat(this.allowed.permits("other@example.com")).isFalse();
		assertThat(this.allowed.permits(null)).isFalse();
	}

	@Test
	void nobodyIsAllowedWhenTheListIsEmpty() {
		assertThat(new AllowedUsers(new String[0]).permits("owner@example.com")).isFalse();
		assertThat(new AllowedUsers(new String[] { "" }).permits("")).isFalse();
	}

	@Test
	void anAllowedVerifiedUserSignsIn() {
		OidcUser user = userOf("owner@example.com", true);
		assertThat(serviceReturning(user).loadUser(null)).isSameAs(user);
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
		assertThatThrownBy(() -> serviceReturning(user).loadUser(null)).isInstanceOf(OAuth2AuthenticationException.class)
			.satisfies((error) -> assertThat(((OAuth2AuthenticationException) error).getError().getErrorCode())
				.isEqualTo(AllowedOidcUserService.UNAUTHORIZED_USER))
			.hasMessageContaining("Unauthorized user");
	}

	private AllowedOidcUserService serviceReturning(OidcUser user) {
		return new AllowedOidcUserService(this.allowed, (request) -> user);
	}

	private OidcUser userOf(String email, boolean verified) {
		Instant now = Instant.now();
		OidcIdToken token = new OidcIdToken("t", now, now.plusSeconds(60),
				Map.of("sub", "1", "email", email, "email_verified", verified));
		return new DefaultOidcUser(java.util.List.of(), token);
	}

}
