package com.simpleblog.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.simpleblog.component.ViewLinks;
import com.simpleblog.config.CacheConfig;
import com.simpleblog.dto.Feed;
import com.simpleblog.dto.FeedEntry;
import com.simpleblog.dto.FeedRow;
import com.simpleblog.entity.PostType;
import com.simpleblog.repository.PostRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The Atom feed: the newest published posts. It is cached (see CacheConfig), so the database is read once per timeout. */
@Service
public class FeedService {

	static final int ENTRY_COUNT = 20;

	private final PostRepository postRepository;

	private final ViewLinks viewLinks;

	private final Clock clock;

	public FeedService(PostRepository postRepository, ViewLinks viewLinks, Clock clock) {
		this.postRepository = postRepository;
		this.viewLinks = viewLinks;
		this.clock = clock;
	}

	/** The newest published posts, with times in the site's zone. Without a post the feed's time is now. */
	@Cacheable(CacheConfig.FEED_CACHE)
	@Transactional(readOnly = true)
	public Feed latestFeed() {
		List<FeedEntry> entries = this.postRepository.listOfFeedRows(PageRequest.of(0, ENTRY_COUNT))
			.stream()
			.map(this::entryOf)
			.toList();
		return new Feed(entries, entries.isEmpty() ? OffsetDateTime.now(this.clock) : entries.getFirst().updated());
	}

	private FeedEntry entryOf(FeedRow row) {
		return new FeedEntry(row.title(), this.viewLinks.pathOf(PostType.POST, row.slug()), localTime(row.createdAt()), localTime(row.modifiedAt()));
	}

	/** The table keeps UTC; readers see the site's zone. */
	private OffsetDateTime localTime(LocalDateTime utc) {
		return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(this.clock.getZone()).toOffsetDateTime();
	}

}
