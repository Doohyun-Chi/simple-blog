package com.simpleblog.component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.simpleblog.entity.BlogUser;
import com.simpleblog.entity.UserSetting;
import com.simpleblog.repository.UserRepository;
import com.simpleblog.repository.UserSettingRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lets only a client with a user's MCP access key into {@code /mcp/**}: the key comes as {@code Authorization: Bearer {key}}
 * and the call then acts as the user it belongs to. A request with an {@code Origin} that the server does not allow is refused
 * first, so a web page cannot use a key. There is no session and no cookie; the user is known for this request only.
 * <p>
 * Not a bean on purpose: Spring Boot would also register a bean filter for every request. SecurityConfig adds it to its chain.
 */
public class McpAccessFilter extends OncePerRequestFilter {

	static final String PATH = "/mcp/";

	private static final String BEARER = "Bearer ";

	private final UserSettingRepository settingRepository;

	private final UserRepository userRepository;

	private final CorsConfiguration allowedOrigins = new CorsConfiguration();

	public McpAccessFilter(UserSettingRepository settingRepository, UserRepository userRepository, String[] allowedOriginPatterns) {
		this.settingRepository = settingRepository;
		this.userRepository = userRepository;
		this.allowedOrigins.setAllowedOriginPatterns(List.of(allowedOriginPatterns));
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith(PATH);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String origin = request.getHeader(HttpHeaders.ORIGIN);
		if (origin != null && this.allowedOrigins.checkOrigin(origin) == null) {
			response.sendError(HttpStatus.FORBIDDEN.value());
			return;
		}
		Optional<BlogUser> user = userOf(request.getHeader(HttpHeaders.AUTHORIZATION));
		if (user.isEmpty()) {
			response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
			response.sendError(HttpStatus.UNAUTHORIZED.value());
			return;
		}
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authenticationOf(user.get()));
		SecurityContextHolder.setContext(context);
		chain.doFilter(request, response);
	}

	/** The user whose current key this is, none for a missing, malformed or replaced key. */
	private Optional<BlogUser> userOf(String authorization) {
		if (authorization == null || !authorization.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
			return Optional.empty();
		}
		String key = authorization.substring(BEARER.length()).trim();
		if (key.isEmpty()) {
			return Optional.empty();
		}
		return this.settingRepository.findByMcpAccessKey(key).map(UserSetting::userId).flatMap(this.userRepository::findById);
	}

	/** An authentication whose principal is the user, in the form the services read the signed-in user from. */
	private Authentication authenticationOf(BlogUser user) {
		Instant now = Instant.now();
		OidcIdToken token = new OidcIdToken("mcp", now, now.plusSeconds(3600), Map.of("sub", String.valueOf(user.id()), "email", user.email()));
		BlogOidcUser principal = new BlogOidcUser(new DefaultOidcUser(List.of(), token), user.id());
		return new McpAuthentication(principal);
	}

	/** The authentication of a request that came with a valid key. */
	private static final class McpAuthentication extends AbstractAuthenticationToken {

		private static final long serialVersionUID = 1L;

		private final BlogOidcUser principal;

		McpAuthentication(BlogOidcUser principal) {
			super(principal.getAuthorities());
			this.principal = principal;
			setAuthenticated(true);
		}

		@Override
		public Object getCredentials() {
			return "";
		}

		@Override
		public Object getPrincipal() {
			return this.principal;
		}

	}

}
