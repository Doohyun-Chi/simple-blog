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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

	private final PostRepository postRepository;

	private final SlugEncoder slugEncoder;

	private final LikePattern likePattern;

	public PostService(PostRepository postRepository, SlugEncoder slugEncoder, LikePattern likePattern) {
		this.postRepository = postRepository;
		this.slugEncoder = slugEncoder;
		this.likePattern = likePattern;
	}

	@Transactional(readOnly = true)
	public PostList listOfPosts(PostQueryType queryType, String query) {
		if (query.isBlank()) {
			throw new InvalidPostQueryException("query must contain at least one non-whitespace character");
		}
		if (queryType != PostQueryType.KEYWORD) {
			throw new UnsupportedPostQueryTypeException(queryType);
		}
		return new PostList(this.postRepository.listOfPublishedPostsContaining(this.likePattern.containing(query)));
	}

	@Transactional(readOnly = true)
	public PostBody postBody(String slug) {
		return this.postRepository.post(this.slugEncoder.encode(slug))
			.map(PostBody::from)
			.orElseThrow(() -> new PostNotFoundException(slug));
	}

}
