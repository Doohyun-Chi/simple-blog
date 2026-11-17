package com.simpleblog.component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Guards the write operations of be/apispec.yaml (the bearerAuth ones). Reads stay public. A request needs
 * {@code Authorization: Bearer <token>} with the token from {@code simpleblog.write.token}: a missing or wrong token is
 * answered with 401. When no token is configured nobody may write, so every write is answered with 403 and the
 * database cannot be changed by an unconfigured deployment.
 * <p>
 * Switched off unless {@code simpleblog.write.access.enabled=true}: until OAuth token validation replaces this static
 * token, the write endpoints are open to every caller. See the project's OAuth issue.
 */
@Component
@ConditionalOnProperty(name = "simpleblog.write.access.enabled", havingValue = "true")
public class WriteAccessInterceptor implements HandlerInterceptor {

	private static final String BEARER = "Bearer ";

	private final byte[] token;

	public WriteAccessInterceptor(@Value("${simpleblog.write.token:}") String token) {
		this.token = token.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (isRead(request.getMethod())) {
			return true;
		}
		if (this.token.length == 0) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}
		if (!hasToken(request.getHeader(HttpHeaders.AUTHORIZATION))) {
			response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}
		return true;
	}

	private boolean isRead(String method) {
		return HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method) || HttpMethod.OPTIONS.matches(method);
	}

	/** Compares in constant time, so the response time does not reveal how much of a guess was right. */
	private boolean hasToken(String authorization) {
		if (authorization == null || !authorization.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
			return false;
		}
		byte[] candidate = authorization.substring(BEARER.length()).getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(this.token, candidate);
	}

}
