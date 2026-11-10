package com.simpleblog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets a FE served from another origin (for example the Vite dev server) call the public API. Origins come from
 * {@code simpleblog.cors.allowed-origin-patterns}, which accepts Spring origin patterns such as
 * {@code http://localhost:[*]} (any port). When it is empty no CORS mapping is registered, so cross-origin calls stay
 * blocked by default.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private static final String PUBLIC_API = "/public/api/**";

	private final String[] allowedOriginPatterns;

	public CorsConfig(@Value("${simpleblog.cors.allowed-origin-patterns:}") String[] allowedOriginPatterns) {
		this.allowedOriginPatterns = allowedOriginPatterns;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		if (this.allowedOriginPatterns.length == 0) {
			return;
		}
		registry.addMapping(PUBLIC_API)
			.allowedOriginPatterns(this.allowedOriginPatterns)
			.allowedMethods("GET", "POST", "PUT", "DELETE");
	}

}
