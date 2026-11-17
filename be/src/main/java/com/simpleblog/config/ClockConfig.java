package com.simpleblog.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The site's clock. WordPress dates in wp_posts are in the site's local time zone, Asia/Seoul. */
@Configuration
public class ClockConfig {

	private static final ZoneId SITE_ZONE = ZoneId.of("Asia/Seoul");

	@Bean
	public Clock siteClock() {
		return Clock.system(SITE_ZONE);
	}

}
