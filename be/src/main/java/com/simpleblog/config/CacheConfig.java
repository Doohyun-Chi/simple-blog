package com.simpleblog.config;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Local, in-memory caches (Caffeine). Today only the feed is cached; it expires a fixed time after it was loaded. */
@Configuration
@EnableCaching
public class CacheConfig {

	public static final String FEED_CACHE = "feed";

	@Bean
	public CacheManager cacheManager(@Value("${simpleblog.feed.cache-timeout:24h}") Duration feedTimeout) {
		CaffeineCacheManager manager = new CaffeineCacheManager(FEED_CACHE);
		manager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(feedTimeout));
		return manager;
	}

}
