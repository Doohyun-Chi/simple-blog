package com.simpleblog.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

import com.simpleblog.component.LikePattern;
import com.simpleblog.component.SlugEncoder;
import com.simpleblog.dto.PostBody;
import com.simpleblog.dto.PostInput;
import com.simpleblog.dto.PostList;
import com.simpleblog.dto.PostQueryType;
import com.simpleblog.entity.Post;
import com.simpleblog.exception.DuplicatePostSlugException;
import com.simpleblog.exception.InvalidPostException;
import com.simpleblog.exception.InvalidPostQueryException;
import com.simpleblog.exception.PostNotFoundException;
import com.simpleblog.exception.UnsupportedPostQueryTypeException;
import com.simpleblog.repository.PostRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

	private static final int MIN_LIMIT = 1;

	private static final int MAX_LIMIT = 100;

	/** English letters and '-' only; no space. */
	private static final Pattern SLUG = Pattern.compile("[A-Za-z-]+");

	private final PostRepository postRepository;

	private final SlugEncoder slugEncoder;

	private final LikePattern likePattern;

	private final Clock clock;

	private final int titleMaxLength;

	private final int slugMaxLength;

	public PostService(PostRepository postRepository, SlugEncoder slugEncoder, LikePattern likePattern, Clock clock,
			@Value("${simpleblog.post.title.max-length}") int titleMaxLength,
			@Value("${simpleblog.post.slug.max-length}") int slugMaxLength) {
		this.postRepository = postRepository;
		this.slugEncoder = slugEncoder;
		this.likePattern = likePattern;
		this.clock = clock;
		this.titleMaxLength = titleMaxLength;
		this.slugMaxLength = slugMaxLength;
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

	/** Creates a draft post authored by the user with this id. The slug must not be used by any row of wp_posts. */
	@Transactional
	public PostBody createPost(long authorId, PostInput input) {
		String slug = storedSlugOf(input);
		if (this.postRepository.slugInUse(slug)) {
			throw new DuplicatePostSlugException(input.slug());
		}
		Post post = Post.draftOf(authorId, input.title(), slug, input.content(), localNow(), gmtNow());
		return PostBody.from(this.postRepository.save(post));
	}

	/** Replaces title, slug and content of the post with the path slug; a different body slug renames it. */
	@Transactional
	public PostBody updatePost(String slug, PostInput input) {
		String newSlug = storedSlugOf(input);
		Post post = this.postRepository.writablePost(this.slugEncoder.encode(slug))
			.orElseThrow(() -> new PostNotFoundException(slug));
		if (this.postRepository.slugInUseExcept(newSlug, post.id())) {
			throw new DuplicatePostSlugException(input.slug());
		}
		post.revise(input.title(), newSlug, input.content(), localNow(), gmtNow());
		return PostBody.from(post);
	}

	/** Now in the site's local time, in whole seconds like the DATETIME columns and the spec's date format. */
	private LocalDateTime localNow() {
		return LocalDateTime.now(this.clock).truncatedTo(ChronoUnit.SECONDS);
	}

	private LocalDateTime gmtNow() {
		return LocalDateTime.now(this.clock.withZone(ZoneOffset.UTC)).truncatedTo(ChronoUnit.SECONDS);
	}

	/** Checks the input against be/apispec.yaml and returns the slug in the form WordPress stores. */
	private String storedSlugOf(PostInput input) {
		requireValidTitle(input.title());
		if (input.content() == null) {
			throw new InvalidPostException("content is required");
		}
		if (input.slug() == null || !SLUG.matcher(input.slug()).matches()) {
			throw new InvalidPostException("slug must be made of English letters and '-' only");
		}
		if (input.slug().length() > this.slugMaxLength) {
			throw new InvalidPostException("slug is too long: it is limited to " + this.slugMaxLength);
		}
		return input.slug();
	}

	private void requireValidTitle(String title) {
		if (title == null || title.isBlank()) {
			throw new InvalidPostException("title must contain a non-whitespace character");
		}
		if (title.codePointCount(0, title.length()) > this.titleMaxLength) {
			throw new InvalidPostException("title is too long: it is limited to " + this.titleMaxLength);
		}
	}

}
