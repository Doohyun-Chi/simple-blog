package com.simpleblog.service;

import java.time.ZoneOffset;
import java.util.List;

import com.simpleblog.component.ViewLinks;
import com.simpleblog.config.CacheConfig;
import com.simpleblog.dto.Sitemap;
import com.simpleblog.dto.SitemapEntry;
import com.simpleblog.dto.SitemapRow;
import com.simpleblog.entity.PostType;
import com.simpleblog.repository.PostRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The sitemap: every published post. It lives in the local cache next to the feed (see CacheConfig); CacheRefresh drops it
 * when a post changes, so the next request builds it again.
 */
@Service
public class SitemapService {

	private final PostRepository postRepository;

	private final ViewLinks viewLinks;

	public SitemapService(PostRepository postRepository, ViewLinks viewLinks) {
		this.postRepository = postRepository;
		this.viewLinks = viewLinks;
	}

	/** The cached sitemap; read from the database only when nothing is cached. */
	@Cacheable(CacheConfig.SITEMAP_CACHE)
	@Transactional(readOnly = true)
	public Sitemap currentSitemap() {
		List<SitemapEntry> posts = this.postRepository.listOfSitemapRows().stream().map(this::entryOf).toList();
		return new Sitemap(posts);
	}

	private SitemapEntry entryOf(SitemapRow row) {
		return new SitemapEntry(this.viewLinks.pathOf(PostType.POST, row.slug()), row.modifiedAt().atOffset(ZoneOffset.UTC));
	}

}
