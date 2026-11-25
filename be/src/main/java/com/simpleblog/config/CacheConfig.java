package com.simpleblog.config;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Local, in-memory caches (Caffeine). The feed and the sitemap are cached; each expires a fixed time after it was loaded. */
@Configuration
@EnableCaching
public class CacheConfig {

	public static final String FEED_CACHE = "feed";

	public static final String SITEMAP_CACHE = "sitemap";

	@Bean
	public CacheManager cacheManager(@Value("${simpleblog.feed.cache-timeout:24h}") Duration cacheTimeout) {
		CaffeineCacheManager manager = new CaffeineCacheManager(FEED_CACHE, SITEMAP_CACHE);
		manager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(cacheTimeout));
		return manager;
	}

}
