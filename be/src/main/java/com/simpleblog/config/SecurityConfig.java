package com.simpleblog.config;

import com.simpleblog.component.AllowedOidcUserService;
import com.simpleblog.component.McpAccessFilter;
import com.simpleblog.repository.UserRepository;
import com.simpleblog.repository.UserSettingRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;

/**
 * Web security. Everything is public except {@code /public/api/v1/me}, which needs a signed-in session. Signing in is
 * the OAuth 2.0 / OpenID Connect authorization code flow with Google, run entirely by Spring Security on the
 * server: {@code /public/auth/google} starts it and {@code /public/auth/google/callback} (the redirect URI
 * registered with Google) finishes it. The frontend only links to the start and reads {@code /me}.
 * <p>
 * Creating and updating posts and registering media need the signed-in session; reads stay public. Because the session cookie authorizes
 * those writes, CSRF protection is on: the token is published in the {@code XSRF-TOKEN} cookie on every response and the
 * frontend echoes it in the {@code X-XSRF-TOKEN} header. A write without it is answered with 403.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	static final String LOGIN_BASE_URI = "/public/auth";

	static final String LOGOUT_URI = "/public/auth/logout";

	static final String CALLBACK_URI = "/public/auth/google/callback";

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectProvider<ClientRegistrationRepository> clients,
			AllowedOidcUserService userService, UserSettingRepository settings, UserRepository users,
			@Value("${simpleblog.auth.frontend-url:}") String frontendUrl, @Value("${simpleblog.csrf.cookie-domain:}") String csrfCookieDomain,
			@Value("${simpleblog.cors.allowed-origin-patterns:}") String[] allowedOriginPatterns) throws Exception {
		http.cors(Customizer.withDefaults())
			.csrf((csrf) -> csrf.csrfTokenRepository(csrfTokens(csrfCookieDomain))
				.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
				.ignoringRequestMatchers("/mcp/**"))
			.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
			.addFilterBefore(new McpAccessFilter(settings, users, allowedOriginPatterns), UsernamePasswordAuthenticationFilter.class)
			.authorizeHttpRequests((requests) -> requests.requestMatchers("/public/api/v1/me")
				.authenticated()
				.requestMatchers("/public/api/v1/medias")
				.authenticated()
				.requestMatchers("/public/api/v1/book-preview")
				.authenticated()
				.requestMatchers("/public/api/v1/user", "/public/api/v1/user/**")
				.authenticated()
				.requestMatchers(HttpMethod.GET, "/public/api/**")
				.permitAll()
				.requestMatchers("/public/api/v1/post", "/public/api/v1/post/**", "/public/api/v1/book", "/public/api/v1/book/**",
					"/public/api/v1/media", "/public/api/v1/media/**")
				.authenticated()
				.anyRequest()
				.permitAll())
			.exceptionHandling((errors) -> errors.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
			.logout((logout) -> logout.logoutUrl(LOGOUT_URI)
				.deleteCookies("JSESSIONID")
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
			.requestCache((cache) -> cache.requestCache(new NullRequestCache()));
		if (clients.getIfAvailable() != null) {
			http.oauth2Login((login) -> googleLogin(login, userService, frontendUrl.replaceAll("/+$", "")));
		}
		return http.build();
	}

	/**
	 * Publishes the token for the frontend's script to read. When the frontend is on another host (www.chidoo.me, with the
	 * backend on blog.chidoo.me) the cookie must be set for their shared parent domain, or the frontend cannot see it.
	 */
	private CookieCsrfTokenRepository csrfTokens(String cookieDomain) {
		CookieCsrfTokenRepository tokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
		if (!cookieDomain.isBlank()) {
			tokens.setCookieCustomizer((cookie) -> cookie.domain(cookieDomain.trim()));
		}
		return tokens;
	}

	/** The sign-in URLs, and where the browser goes afterwards: the frontend's home page, flagged when it failed. */
	private void googleLogin(OAuth2LoginConfigurer<HttpSecurity> login, AllowedOidcUserService userService, String frontendUrl) {
		login.authorizationEndpoint((endpoint) -> endpoint.baseUri(LOGIN_BASE_URI))
			.redirectionEndpoint((endpoint) -> endpoint.baseUri(CALLBACK_URI))
			.userInfoEndpoint((endpoint) -> endpoint.oidcUserService(userService))
			.defaultSuccessUrl(frontendUrl + "/", true)
			.failureHandler(failureRedirect(frontendUrl));
	}

	/**
	 * Redirects a failed sign-in without creating a session, so forged callbacks cannot make the server keep any. A
	 * user who is not allowed gets {@code login=unauthorized}, every other failure {@code login=failed}.
	 */
	private AuthenticationFailureHandler failureRedirect(String frontendUrl) {
		SimpleUrlAuthenticationFailureHandler failed = redirectTo(frontendUrl + "/?login=failed");
		SimpleUrlAuthenticationFailureHandler unauthorized = redirectTo(frontendUrl + "/?login=unauthorized");
		return (request, response, exception) -> {
			boolean notAllowed = exception instanceof OAuth2AuthenticationException oauth
					&& AllowedOidcUserService.UNAUTHORIZED_USER.equals(oauth.getError().getErrorCode());
			(notAllowed ? unauthorized : failed).onAuthenticationFailure(request, response, exception);
		};
	}

	private SimpleUrlAuthenticationFailureHandler redirectTo(String url) {
		SimpleUrlAuthenticationFailureHandler handler = new SimpleUrlAuthenticationFailureHandler(url);
		handler.setAllowSessionCreation(false);
		return handler;
	}

}
