package com.simpleblog.service;

import java.time.Clock;
import java.util.List;

import com.simpleblog.component.LikePattern;
import com.simpleblog.component.PostExcerpt;
import com.simpleblog.dto.PostList;
import com.simpleblog.dto.PostListRequest;
import com.simpleblog.dto.PostListView;
import com.simpleblog.dto.PostQueryType;
import com.simpleblog.dto.PostSummary;
import com.simpleblog.entity.Post;
import com.simpleblog.exception.InvalidPostQueryException;
import com.simpleblog.exception.UnsupportedPostQueryTypeException;
import com.simpleblog.repository.OffsetPage;
import com.simpleblog.repository.PostRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The post list of be/apispec.yaml: search, paging and, for the card view, a thumbnail and an excerpt of each post. */
@Service
public class PostListService {

	private static final int MIN_LIMIT = 1;

	private static final int MAX_LIMIT = 100;

	private final PostRepository postRepository;

	private final LikePattern likePattern;

	private final PostExcerpt excerpt;

	private final Clock clock;

	public PostListService(PostRepository postRepository, LikePattern likePattern, PostExcerpt excerpt, Clock clock) {
		this.postRepository = postRepository;
		this.likePattern = likePattern;
		this.excerpt = excerpt;
		this.clock = clock;
	}

	/**
	 * The published posts plus every post of the viewer whatever its status, newest first, from the {@code offset}th on. Without a
	 * query every one of them matches. Without a limit every match is returned and there is no next one.
	 */
	@Transactional(readOnly = true)
	public PostList listOfPosts(long viewerId, PostListRequest request) {
		validate(request);
		String pattern = this.likePattern.containing(request.query() == null ? "" : request.query());
		List<PostSummary> rows = rowsOf(pattern, viewerId, request).stream().map((summary) -> summary.inZone(this.clock.getZone())).toList();
		boolean hasNext = request.limit() != null && rows.size() > request.limit();
		return new PostList(hasNext ? rows.subList(0, request.limit()) : rows, hasNext);
	}

	private void validate(PostListRequest request) {
		if (request.query() != null && request.query().isBlank()) {
			throw new InvalidPostQueryException("query must contain at least one non-whitespace character");
		}
		if (request.queryType() != PostQueryType.KEYWORD) {
			throw new UnsupportedPostQueryTypeException(request.queryType());
		}
		if (request.limit() != null && (request.limit() < MIN_LIMIT || request.limit() > MAX_LIMIT)) {
			throw new InvalidPostQueryException("limit must be from " + MIN_LIMIT + " to " + MAX_LIMIT);
		}
		if (request.offset() < 0) {
			throw new InvalidPostQueryException("offset must not be negative");
		}
	}

	/** One row more than the limit, to tell whether another one follows. */
	private List<PostSummary> rowsOf(String pattern, long viewerId, PostListRequest request) {
		Pageable page = new OffsetPage(request.offset(), request.limit() == null ? Integer.MAX_VALUE : request.limit() + 1);
		if (request.view() == PostListView.CARD) {
			return this.postRepository.listOfPostCardsContaining(pattern, viewerId, page)
				.stream()
				.map((post) -> cardOf(post, request.origin()))
				.toList();
		}
		return this.postRepository.listOfPostsContaining(pattern, viewerId, page);
	}

	private PostSummary cardOf(Post post, String origin) {
		return new PostSummary(post.id(), post.title(), post.slug(), post.status(), post.createdAt(),
				this.excerpt.firstImageOf(post.content(), origin).orElse(null), this.excerpt.descriptionOf(post.content()));
	}

}
