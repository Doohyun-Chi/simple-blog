package com.simpleblog.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;

import com.simpleblog.component.CacheRefresh;
import com.simpleblog.component.SlugEncoder;
import com.simpleblog.dto.PostBody;
import com.simpleblog.dto.PostInput;
import com.simpleblog.entity.Post;
import com.simpleblog.entity.PostStatus;
import com.simpleblog.entity.UserSetting;
import com.simpleblog.exception.DuplicatePostSlugException;
import com.simpleblog.exception.InvalidPostException;
import com.simpleblog.exception.NotPostAuthorException;
import com.simpleblog.exception.PostNotFoundException;
import com.simpleblog.repository.PostRepository;
import com.simpleblog.repository.UserSettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {


	/** English letters, digits and '-' only; no space. */
	private static final Pattern SLUG = Pattern.compile("[A-Za-z0-9-]+");

	private final PostRepository postRepository;

	private final SlugEncoder slugEncoder;

	private final UserService userService;

	private final CacheRefresh cacheRefresh;

	private final MediaService mediaService;

	private final BookmarkService bookmarkService;

	private final UserSettingRepository settingRepository;

	private final Clock clock;

	/** The most slugs one save may name in PostInput.replacedMedia, as in be/apispec.yaml. */
	private static final int REPLACED_MEDIA_MAX = 100;

	/** The size of TB_POST.HASH_TAGS, VARCHAR(200) in be/db/TB_POST.sql. */
	private static final int HASH_TAGS_MAX_LENGTH = 200;

	private final int titleMaxLength;

	private final int slugMaxLength;

	public PostService(PostRepository postRepository, SlugEncoder slugEncoder, UserService userService,
			CacheRefresh cacheRefresh, MediaService mediaService, BookmarkService bookmarkService,
			UserSettingRepository settingRepository, Clock clock, @Value("${simpleblog.post.title.max-length}") int titleMaxLength,
			@Value("${simpleblog.post.slug.max-length}") int slugMaxLength) {
		this.postRepository = postRepository;
		this.slugEncoder = slugEncoder;
		this.userService = userService;
		this.cacheRefresh = cacheRefresh;
		this.mediaService = mediaService;
		this.bookmarkService = bookmarkService;
		this.settingRepository = settingRepository;
		this.clock = clock;
		this.titleMaxLength = titleMaxLength;
		this.slugMaxLength = slugMaxLength;
	}

	/** A published post to anyone; a draft or private one only to its author, to anyone else it is as if it did not exist. */
	@Transactional(readOnly = true)
	public PostBody postBody(String slug) {
		long viewerId = this.userService.currentViewerId();
		return this.postRepository.post(this.slugEncoder.encode(slug))
			.filter((post) -> post.status() == PostStatus.PUBLISH || post.editableBy(viewerId))
			.map((post) -> PostBody.from(post, this.clock.getZone(), viewerId, bookmarkIdOf(viewerId, post)))
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
		Post post = Post.draftOf(authorId, input.title(), slug, input.content(), input.hashTags(), utcNow());
		post.signedAs(nickOf(authorId));
		applyStatus(post, input);
		Post saved = this.postRepository.save(post);
		this.mediaService.discardUnused(authorId, replacedMediaOf(input));
		this.cacheRefresh.afterPostChange();
		return PostBody.from(saved, this.clock.getZone(), authorId, null);
	}

	/**
	 * Replaces title, slug and content of the post with the path slug; a different body slug renames it. Only its author,
	 * taken from the session of the current request, may do it.
	 */
	@Transactional
	public PostBody updatePost(String slug, PostInput input) {
		String newSlug = storedSlugOfUpdate(slug, input);
		Post post = this.postRepository.post(this.slugEncoder.encode(slug))
			.orElseThrow(() -> new PostNotFoundException(slug));
		long viewerId = this.userService.currentViewerId();
		if (!post.editableBy(viewerId)) {
			throw new NotPostAuthorException(slug);
		}
		if (this.postRepository.slugInUseExcept(newSlug, post.id())) {
			throw new DuplicatePostSlugException(input.slug());
		}
		if (!newSlug.equals(post.slug())) {
			this.bookmarkService.follow(post.id(), newSlug);
		}
		post.revise(input.title(), newSlug, input.content(), input.hashTags(), utcNow());
		post.signedAs(nickOf(viewerId));
		applyStatus(post, input);
		this.mediaService.discardUnused(viewerId, replacedMediaOf(input));
		this.cacheRefresh.afterPostChange();
		return PostBody.from(post, this.clock.getZone(), viewerId, bookmarkIdOf(viewerId, post));
	}

	/**
	 * Deletes the post or page with this slug on behalf of the user of the current session, who must be its author.
	 */
	@Transactional
	public void deletePost(String slug) {
		Post post = this.postRepository.post(this.slugEncoder.encode(slug))
			.orElseThrow(() -> new PostNotFoundException(slug));
		if (!post.editableBy(this.userService.currentViewerId())) {
			throw new NotPostAuthorException(slug);
		}
		this.bookmarkService.removeOfPost(post.id());
		this.postRepository.delete(post);
		this.cacheRefresh.afterPostChange();
	}

	/** The nick name the user has set, null when there is none. */
	private String nickOf(long userId) {
		return this.settingRepository.findById(userId).map(UserSetting::nick).orElse(null);
	}

	/** The requester's bookmark of the post; an anonymous visitor has none. */
	private Long bookmarkIdOf(long viewerId, Post post) {
		return viewerId == UserService.ANONYMOUS ? null : this.bookmarkService.bookmarkIdOf(viewerId, post.id());
	}

	/** The images the input names as replaced, none when it names none. Checked with the other fields; see requireValidFields. */
	private List<String> replacedMediaOf(PostInput input) {
		return input.replacedMedia() == null ? List.of() : input.replacedMedia();
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

	/**
	 * Like {@link #storedSlugOf}, except that a body slug equal to the path slug keeps the slug as it is stored without the
	 * slug rules, so a row migrated from WordPress with an underscore or non-ASCII characters in its slug can still be updated.
	 */
	private String storedSlugOfUpdate(String pathSlug, PostInput input) {
		if (input.slug() == null || !this.slugEncoder.encode(input.slug()).equals(this.slugEncoder.encode(pathSlug))) {
			return storedSlugOf(input);
		}
		requireValidFields(input);
		return this.slugEncoder.encode(pathSlug);
	}

	/** Checks the input against be/apispec.yaml and returns the slug in the stored form. */
	private String storedSlugOf(PostInput input) {
		requireValidFields(input);
		if (input.slug() == null || !SLUG.matcher(input.slug()).matches()) {
			throw new InvalidPostException("slug must be made of English letters, digits and '-' only");
		}
		if (input.slug().length() > this.slugMaxLength) {
			throw new InvalidPostException("slug is too long: it is limited to " + this.slugMaxLength);
		}
		return input.slug();
	}

	private void requireValidFields(PostInput input) {
		requireValidTitle(input.title());
		requireValidHashTags(input.hashTags());
		requireValidStatus(input.status());
		if (replacedMediaOf(input).size() > REPLACED_MEDIA_MAX) {
			throw new InvalidPostException("replacedMedia names too many images: it is limited to " + REPLACED_MEDIA_MAX);
		}
		if (input.content() == null) {
			throw new InvalidPostException("content is required");
		}
	}

	private void requireValidStatus(PostStatus status) {
		if (status != null && !status.settable()) {
			throw new InvalidPostException("status must be draft or publish");
		}
	}

	private void requireValidHashTags(String hashTags) {
		if (hashTags != null && hashTags.codePointCount(0, hashTags.length()) > HASH_TAGS_MAX_LENGTH) {
			throw new InvalidPostException("hashTags is too long: it is limited to " + HASH_TAGS_MAX_LENGTH);
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
