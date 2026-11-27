package com.simpleblog.component;

import java.io.IOException;
import java.util.function.LongPredicate;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Brings the browser back to the page it signed in from. {@link #remembering} keeps the {@code returnTo} of the sign-in start
 * request in the session; {@link #redirecting} is the success handler that sends the browser there. Not beans: the security
 * configuration builds them.
 */
public final class LoginReturn {

	public static final String ATTRIBUTE = LoginReturn.class.getName() + ".PATH";

	/** The personalization page of the frontend. */
	private static final String USER_PATH = "/user";

	private static final int MAX_LENGTH = 2000;

	/** A path of the frontend: one leading slash, then no backslash, whitespace or control character. */
	private static final Pattern PATH = Pattern.compile("/(?!/)[^\\\\\\s\\p{Cntrl}]*|/");

	private LoginReturn() {
	}

	/** The filter that remembers {@code returnTo} when the request starts the sign-in at {@code startUri}. */
	public static OncePerRequestFilter remembering(String startUri) {
		return new OncePerRequestFilter() {
			@Override
			protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
					throws ServletException, IOException {
				String path = request.getParameter("returnTo");
				if (startUri.equals(request.getRequestURI()) && isPath(path)) {
					request.getSession().setAttribute(ATTRIBUTE, path);
				}
				chain.doFilter(request, response);
			}
		};
	}

	/**
	 * The success handler: a user who has not saved a personalization yet goes to the personalization page; anyone else to the
	 * remembered page of the frontend, else to its home page. {@code isNew} tells whether a user id belongs to a new user.
	 */
	public static AuthenticationSuccessHandler redirecting(String frontendUrl, LongPredicate isNew) {
		return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
			HttpSession session = request.getSession(false);
			Object path = session == null ? null : session.getAttribute(ATTRIBUTE);
			if (session != null) {
				session.removeAttribute(ATTRIBUTE);
			}
			boolean newUser = authentication != null && authentication.getPrincipal() instanceof BlogOidcUser user && isNew.test(user.userId());
			response.sendRedirect(frontendUrl + (newUser ? USER_PATH : path instanceof String text ? text : "/"));
		};
	}

	private static boolean isPath(String value) {
		return value != null && value.length() <= MAX_LENGTH && PATH.matcher(value).matches();
	}

}
