package com.simpleblog.component;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoginReturnTests {

	private static final String START = "/public/auth/google";

	private MockHttpServletRequest startRequest(String uri, String returnTo) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
		request.setRequestURI(uri);
		if (returnTo != null) {
			request.setParameter("returnTo", returnTo);
		}
		return request;
	}

	private Object remembered(String uri, String returnTo) throws ServletException, IOException {
		MockHttpServletRequest request = startRequest(uri, returnTo);
		LoginReturn.remembering(START).doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
		return request.getSession(false) == null ? null : request.getSession().getAttribute(LoginReturn.ATTRIBUTE);
	}

	@ParameterizedTest
	@ValueSource(strings = { "/", "/post/hello", "/search?query=KPI&queryType=keyword", "/post/%ed%81%b0" })
	void remembersAPathOfTheFrontend(String path) throws Exception {
		assertThat(remembered(START, path)).isEqualTo(path);
	}

	@ParameterizedTest
	@ValueSource(strings = { "//evil.com", "https://evil.com", "evil.com", "/a\\b", "/a b", "/a\nb", "" })
	void ignoresAnythingThatIsNotAPath(String path) throws Exception {
		assertThat(remembered(START, path)).isNull();
	}

	@Test
	void ignoresATooLongPathAndOtherRequests() throws Exception {
		assertThat(remembered(START, "/" + "a".repeat(2000))).isNull();
		assertThat(remembered("/public/api/v1/posts", "/post/hello")).isNull();
		assertThat(remembered(START, null)).isNull();
	}

	@Test
	void redirectsToTheRememberedPageAndForgetsIt() throws Exception {
		MockHttpServletRequest request = startRequest("/public/auth/google/callback", null);
		request.getSession().setAttribute(LoginReturn.ATTRIBUTE, "/post/hello");
		MockHttpServletResponse response = new MockHttpServletResponse();
		LoginReturn.redirecting("http://frontend.test", (id) -> false).onAuthenticationSuccess(request, response, signedIn(7));
		assertThat(response.getRedirectedUrl()).isEqualTo("http://frontend.test/post/hello");
		assertThat(request.getSession().getAttribute(LoginReturn.ATTRIBUTE)).isNull();
	}

	@Test
	void redirectsToTheHomePageWhenNothingIsRemembered() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		LoginReturn.redirecting("http://frontend.test", (id) -> false).onAuthenticationSuccess(startRequest("/cb", null), response, signedIn(7));
		assertThat(response.getRedirectedUrl()).isEqualTo("http://frontend.test/");
	}

	@Test
	void sendsANewUserToThePersonalizationPageWhateverPageTheyCameFrom() throws Exception {
		MockHttpServletRequest request = startRequest("/cb", null);
		request.getSession().setAttribute(LoginReturn.ATTRIBUTE, "/post/hello");
		MockHttpServletResponse response = new MockHttpServletResponse();
		LoginReturn.redirecting("http://frontend.test", (id) -> id == 7).onAuthenticationSuccess(request, response, signedIn(7));
		assertThat(response.getRedirectedUrl()).isEqualTo("http://frontend.test/user");
		assertThat(request.getSession().getAttribute(LoginReturn.ATTRIBUTE)).isNull();
	}

	@Test
	void sendsAUserWhoHasSavedBackToThePageTheyCameFrom() throws Exception {
		MockHttpServletRequest request = startRequest("/cb", null);
		request.getSession().setAttribute(LoginReturn.ATTRIBUTE, "/post/hello");
		MockHttpServletResponse response = new MockHttpServletResponse();
		LoginReturn.redirecting("http://frontend.test", (id) -> id == 8).onAuthenticationSuccess(request, response, signedIn(7));
		assertThat(response.getRedirectedUrl()).isEqualTo("http://frontend.test/post/hello");
	}

	private Authentication signedIn(long userId) {
		OidcIdToken token = OidcIdToken.withTokenValue("t").subject("s").claim("email", "a@example.com").build();
		return new TestingAuthenticationToken(new BlogOidcUser(new DefaultOidcUser(List.of(), token), userId), null);
	}

}
