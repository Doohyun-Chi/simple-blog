package com.simpleblog.component;

import com.simpleblog.config.CacheConfig;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Drops the cached feed so the next request reads the posts again. Called when a post is created, changed or deleted.
 * Inside a transaction it waits for the commit: dropping it earlier could let a request in between cache the old posts
 * again. A rolled back transaction changed nothing, so it leaves the cache alone.
 */
@Component
public class FeedRefresh {

	private final CacheManager cacheManager;

	public FeedRefresh(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public void afterPostChange() {
		runAfterCommit(this::dropFeed);
	}

	/** Runs the action once the transaction commits, or right away when there is no transaction. */
	private static void runAfterCommit(Runnable action) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			action.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				action.run();
			}
		});
	}

	private void dropFeed() {
		this.cacheManager.getCache(CacheConfig.FEED_CACHE).clear();
	}

}
