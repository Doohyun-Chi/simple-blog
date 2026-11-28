package com.simpleblog.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.simpleblog.component.SlugEncoder;
import com.simpleblog.dto.BookmarkArrangement;
import com.simpleblog.dto.BookmarkInfo;
import com.simpleblog.dto.BookmarkList;
import com.simpleblog.entity.Bookmark;
import com.simpleblog.entity.Post;
import com.simpleblog.entity.PostStatus;
import com.simpleblog.exception.BookmarkNotFoundException;
import com.simpleblog.exception.DuplicateBookmarkException;
import com.simpleblog.exception.InvalidBookmarkException;
import com.simpleblog.exception.PostNotFoundException;
import com.simpleblog.repository.BookmarkRepository;
import com.simpleblog.repository.OffsetPage;
import com.simpleblog.repository.PostRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The bookmarks of a user as in be/apispec.yaml: listing, adding, renaming, arranging and deleting, and following the post they point to. */
@Service
public class BookmarkService {

	static final int NAME_MAX = 130;

	private static final int MIN_LIMIT = 1;

	private static final int MAX_LIMIT = 100;

	private static final int ARRANGE_MAX = 500;

	private final BookmarkRepository bookmarkRepository;

	private final PostRepository postRepository;

	private final SlugEncoder slugEncoder;

	private final Clock clock;

	public BookmarkService(BookmarkRepository bookmarkRepository, PostRepository postRepository, SlugEncoder slugEncoder, Clock clock) {
		this.bookmarkRepository = bookmarkRepository;
		this.postRepository = postRepository;
		this.slugEncoder = slugEncoder;
		this.clock = clock;
	}

	/** The user's bookmarks, the pinned first; at most {@code limit} of them when it is given. */
	@Transactional(readOnly = true)
	public BookmarkList listOfBookmarks(long ownerId, Integer limit) {
		if (limit != null && (limit < MIN_LIMIT || limit > MAX_LIMIT)) {
			throw new InvalidBookmarkException("limit must be from " + MIN_LIMIT + " to " + MAX_LIMIT);
		}
		Pageable page = limit == null ? Pageable.unpaged() : new OffsetPage(0, limit);
		return infoOf(this.bookmarkRepository.listOfBookmarksOf(ownerId, page));
	}

	/** Bookmarks the post with this slug, which the user must be able to read, under its title. */
	@Transactional
	public BookmarkInfo createBookmark(long ownerId, String slug) {
		if (slug == null || slug.isBlank()) {
			throw new InvalidBookmarkException("slug is required");
		}
		Post post = this.postRepository.post(this.slugEncoder.encode(slug))
			.filter((found) -> found.status() == PostStatus.PUBLISH || found.editableBy(ownerId))
			.orElseThrow(() -> new PostNotFoundException(slug));
		if (this.bookmarkRepository.existsByOwnerIdAndPostId(ownerId, post.id())) {
			throw new DuplicateBookmarkException("The post is bookmarked already: " + slug);
		}
		Bookmark saved = this.bookmarkRepository.save(Bookmark.of(ownerId, post, nameOfTitle(post.title()), utcNow()));
		return BookmarkInfo.from(saved, this.clock.getZone());
	}

	/** Gives the bookmark a new name, at once; that is a modification of it. */
	@Transactional
	public BookmarkInfo rename(long ownerId, long id, String name) {
		Bookmark bookmark = owned(ownerId, id);
		bookmark.rename(validName(name), utcNow());
		return BookmarkInfo.from(bookmark, this.clock.getZone());
	}

	/** Deletes the bookmark; the post stays. */
	@Transactional
	public void delete(long ownerId, long id) {
		this.bookmarkRepository.delete(owned(ownerId, id));
	}

	/** Saves the names and orders of several bookmarks all or none; the modification time is left as it is. */
	@Transactional
	public BookmarkList save(long ownerId, BookmarkArrangement arrangement) {
		List<BookmarkArrangement.Item> items = validItems(arrangement);
		List<Bookmark> found = this.bookmarkRepository.listOfBookmarksIn(ownerId, items.stream().map(BookmarkArrangement.Item::id).toList());
		if (found.size() != items.size()) {
			throw new BookmarkNotFoundException("Some of the bookmarks are not the user's");
		}
		items.forEach((item) -> found.stream().filter((bookmark) -> bookmark.id().equals(item.id())).findFirst()
			.orElseThrow().arrange(item.name().trim(), item.order()));
		return infoOf(found.stream().sorted(BookmarkService::byDisplay).toList());
	}

	/** The id of the user's bookmark of the post, or null when there is none. */
	@Transactional(readOnly = true)
	public Long bookmarkIdOf(long ownerId, long postId) {
		return this.bookmarkRepository.findByOwnerIdAndPostId(ownerId, postId).map(Bookmark::id).orElse(null);
	}

	/** Gives the bookmarks of a post the slug it has been renamed to. */
	@Transactional
	public void follow(long postId, String storedSlug) {
		this.bookmarkRepository.follow(postId, storedSlug);
	}

	/** Removes the bookmarks of a post that is deleted. */
	@Transactional
	public void removeOfPost(long postId) {
		this.bookmarkRepository.deleteOfPost(postId);
	}

	private static int byDisplay(Bookmark first, Bookmark second) {
		int byOrder = Integer.compare(second.sortOrder(), first.sortOrder());
		return byOrder != 0 ? byOrder : Long.compare(second.id(), first.id());
	}

	private Bookmark owned(long ownerId, long id) {
		return this.bookmarkRepository.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> new BookmarkNotFoundException("No bookmark " + id));
	}

	private BookmarkList infoOf(List<Bookmark> bookmarks) {
		return new BookmarkList(bookmarks.stream().map((bookmark) -> BookmarkInfo.from(bookmark, this.clock.getZone())).toList());
	}

	/** A title that is longer than a name is cut at the limit. */
	private String nameOfTitle(String title) {
		int length = title.codePointCount(0, title.length());
		return length <= NAME_MAX ? title : title.substring(0, title.offsetByCodePoints(0, NAME_MAX));
	}

	private List<BookmarkArrangement.Item> validItems(BookmarkArrangement arrangement) {
		List<BookmarkArrangement.Item> items = arrangement.bookmarks();
		if (items.size() > ARRANGE_MAX) {
			throw new InvalidBookmarkException("bookmarks must hold at most " + ARRANGE_MAX + " items");
		}
		Set<Long> ids = new HashSet<>();
		items.forEach((item) -> validItem(item, ids));
		return items;
	}

	/** An item needs an id no other item has. */
	private void validItem(BookmarkArrangement.Item item, Set<Long> ids) {
		if (!ids.add(item.id())) {
			throw new InvalidBookmarkException("every item needs its own id");
		}
	}

	private String validName(String name) {
		String text = name == null ? "" : name.trim();
		if (text.isEmpty() || text.codePointCount(0, text.length()) > NAME_MAX) {
			throw new InvalidBookmarkException("name must be from 1 to " + NAME_MAX + " characters");
		}
		return text;
	}

	private LocalDateTime utcNow() {
		return LocalDateTime.now(this.clock.withZone(ZoneOffset.UTC)).truncatedTo(ChronoUnit.SECONDS);
	}

}
