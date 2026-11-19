package com.simpleblog.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;
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
import com.simpleblog.exception.NotPostAuthorException;
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

	/** The statuses a client may set. */
	private static final Set<String> STATUSES = Set.of("draft", "publish");

	private static final int MIN_LIMIT = 1;

	private static final int MAX_LIMIT = 100;

	/** English letters and '-' only; no space. */
	private static final Pattern SLUG = Pattern.compile("[A-Za-z-]+");

	private final PostRepository postRepository;

	private final SlugEncoder slugEncoder;

	private final LikePattern likePattern;

	private final UserService userService;

	private final Clock clock;

	private final int titleMaxLength;

	private final int slugMaxLength;

	public PostService(PostRepository postRepository, SlugEncoder slugEncoder, LikePattern likePattern, UserService userService,
			Clock clock, @Value("${simpleblog.post.title.max-length}") int titleMaxLength,
			@Value("${simpleblog.post.slug.max-length}") int slugMaxLength) {
		this.postRepository = postRepository;
		this.slugEncoder = slugEncoder;
		this.likePattern = likePattern;
		this.userService = userService;
		this.clock = clock;
		this.titleMaxLength = titleMaxLength;
		this.slugMaxLength = slugMaxLength;
	}

	/** The published posts plus every post of the viewer whatever its status. Without a query every one of them matches. Without a limit every match is returned. */
	@Transactional(readOnly = true)
	public PostList listOfPosts(long viewerId, PostQueryType queryType, String query, Integer limit) {
		if (query != null && query.isBlank()) {
			throw new InvalidPostQueryException("query must contain at least one non-whitespace character");
		}
		if (queryType != PostQueryType.KEYWORD) {
			throw new UnsupportedPostQueryTypeException(queryType);
		}
		String pattern = this.likePattern.containing(query == null ? "" : query);
		return new PostList(this.postRepository.listOfPostsContaining(pattern, viewerId, pageOf(limit))
			.stream()
			.map((summary) -> summary.inZone(this.clock.getZone()))
			.toList());
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
		long viewerId = this.userService.currentViewerId();
		return this.postRepository.post(this.slugEncoder.encode(slug))
			.map((post) -> PostBody.from(post, this.clock.getZone(), viewerId))
			.orElseThrow(() -> new PostNotFoundException(slug));
	}

	/** Creates a draft post authored by the user of the current session. The slug must not be used by any row of TB_POST. */
	@Transactional
	public PostBody createPost(PostInput input) {
		long authorId = this.userService.currentWriterId();
		String slug = storedSlugOf(input);
		if (this.postRepository.slugInUse(slug)) {
			throw new DuplicatePostSlugException(input.slug());
		}
		Post post = Post.draftOf(authorId, input.title(), slug, input.content(), utcNow());
		applyStatus(post, input);
		return PostBody.from(this.postRepository.save(post), this.clock.getZone(), authorId);
	}

	/**
	 * Replaces title, slug and content of the post with the path slug; a different body slug renames it. Only its author,
	 * taken from the session of the current request, may do it.
	 */
	@Transactional
	public PostBody updatePost(String slug, PostInput input) {
		String newSlug = storedSlugOf(input);
		Post post = this.postRepository.writablePost(this.slugEncoder.encode(slug))
			.orElseThrow(() -> new PostNotFoundException(slug));
		long viewerId = this.userService.currentViewerId();
		if (!post.editableBy(viewerId)) {
			throw new NotPostAuthorException(slug);
		}
		if (this.postRepository.slugInUseExcept(newSlug, post.id())) {
			throw new DuplicatePostSlugException(input.slug());
		}
		post.revise(input.title(), newSlug, input.content(), utcNow());
		applyStatus(post, input);
		return PostBody.from(post, this.clock.getZone(), viewerId);
	}

	/**
	 * Deletes the post with this slug on behalf of the user of the current session, who must be its author. Only rows of
	 * type post can be deleted, never pages, attachments or revisions.
	 */
	@Transactional
	public void deletePost(String slug) {
		Post post = this.postRepository.writablePost(this.slugEncoder.encode(slug))
			.orElseThrow(() -> new PostNotFoundException(slug));
		if (!post.editableBy(this.userService.currentViewerId())) {
			throw new NotPostAuthorException(slug);
		}
		this.postRepository.delete(post);
	}

	/** Applies the status of the input when it has one. */
	private void applyStatus(Post post, PostInput input) {
		if (input.status() != null) {
			post.moveTo(input.status());
		}
	}

	/** Now in UTC, in whole seconds like the spec's date format. */
	private LocalDateTime utcNow() {
		return LocalDateTime.now(this.clock.withZone(ZoneOffset.UTC)).truncatedTo(ChronoUnit.SECONDS);
	}

	/** Checks the input against be/apispec.yaml and returns the slug in the stored form. */
	private String storedSlugOf(PostInput input) {
		requireValidTitle(input.title());
		requireValidStatus(input.status());
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

	private void requireValidStatus(String status) {
		if (status != null && !STATUSES.contains(status)) {
			throw new InvalidPostException("status must be draft or publish");
		}
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
