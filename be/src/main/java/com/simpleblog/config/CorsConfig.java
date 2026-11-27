package com.simpleblog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets a FE served from another origin (for example the Vite dev server) call the public API and sign out. Origins come from
 * {@code simpleblog.cors.allowed-origin-patterns}, which accepts Spring origin patterns such as
 * {@code http://localhost:[*]} (any port). When it is empty no CORS mapping is registered, so cross-origin calls stay
 * blocked by default. Credentials are allowed so the frontend can send the session cookie to {@code /me}.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private static final String[] CROSS_ORIGIN_PATHS = { "/public/api/**", SecurityConfig.LOGOUT_URI };

	private final String[] allowedOriginPatterns;

	public CorsConfig(@Value("${simpleblog.cors.allowed-origin-patterns:}") String[] allowedOriginPatterns) {
		this.allowedOriginPatterns = allowedOriginPatterns;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		if (this.allowedOriginPatterns.length == 0) {
			return;
		}
		for (String path : CROSS_ORIGIN_PATHS) {
			registry.addMapping(path)
				.allowedOriginPatterns(this.allowedOriginPatterns)
				.allowedMethods("GET", "POST", "PUT", "DELETE")
				.allowCredentials(true);
		}
	}

}
