package com.simpleblog.service;

import com.simpleblog.component.LikePattern;
import com.simpleblog.component.SlugEncoder;
import com.simpleblog.dto.PostBody;
import com.simpleblog.dto.PostList;
import com.simpleblog.dto.PostQueryType;
import com.simpleblog.exception.InvalidPostQueryException;
import com.simpleblog.exception.PostNotFoundException;
import com.simpleblog.exception.UnsupportedPostQueryTypeException;
import com.simpleblog.repository.PostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

	private static final int MIN_LIMIT = 1;

	private static final int MAX_LIMIT = 100;

	private final PostRepository postRepository;

	private final SlugEncoder slugEncoder;

	private final LikePattern likePattern;

	public PostService(PostRepository postRepository, SlugEncoder slugEncoder, LikePattern likePattern) {
		this.postRepository = postRepository;
		this.slugEncoder = slugEncoder;
		this.likePattern = likePattern;
	}

	/** Without a query every published post matches. Without a limit every match is returned. */
	@Transactional(readOnly = true)
	public PostList listOfPosts(PostQueryType queryType, String query, Integer limit) {
		if (query != null && query.isBlank()) {
			throw new InvalidPostQueryException("query must contain at least one non-whitespace character");
		}
		if (queryType != PostQueryType.KEYWORD) {
			throw new UnsupportedPostQueryTypeException(queryType);
		}
		String pattern = this.likePattern.containing(query == null ? "" : query);
		return new PostList(this.postRepository.listOfPublishedPostsContaining(pattern, pageOf(limit)));
	}

	private Pageable pageOf(Integer limit) {
		if (limit == null) {
			return Pageable.unpaged();
		}
		if (limit < MIN_LIMIT || limit > MAX_LIMIT) {
			throw new InvalidPostQueryException("limit must be from " + MIN_LIMIT + " to " + MAX_LIMIT);
		}
		return PageRequest.ofSize(limit);
	}

	@Transactional(readOnly = true)
	public PostBody postBody(String slug) {
		return this.postRepository.post(this.slugEncoder.encode(slug))
			.map(PostBody::from)
			.orElseThrow(() -> new PostNotFoundException(slug));
	}

}
