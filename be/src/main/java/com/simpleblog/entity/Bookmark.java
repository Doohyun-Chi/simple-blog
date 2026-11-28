package com.simpleblog.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The {@code TB_BOOKMARK} table of DBSCHEMA.md: a post a user keeps in their bookmarks. {@code sortOrder} 0 is a bookmark that is not
 * pinned, above 0 it is pinned. {@code postSlug} is the stored slug of the post, copied so a list needs no join. Both datetimes are
 * UTC; {@code modifiedAt} is null until the bookmark is renamed.
 */
@Entity
@Table(name = "TB_BOOKMARK")
public class Bookmark {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

	@Column(name = "OWNER_ID")
	private Long ownerId;

	@Column(name = "POST_ID")
	private Long postId;

	@Column(name = "POST_SLUG")
	private String postSlug;

	@Column(name = "NAME")
	private String name;

	@Column(name = "SORT_ORDER")
	private int sortOrder;

	@Column(name = "CREATED_DATE")
	private LocalDateTime createdAt;

	@Column(name = "MODIFIED_DATE")
	private LocalDateTime modifiedAt;

	protected Bookmark() {
	}

	/** A bookmark of the post, not pinned, named as given. */
	public static Bookmark of(long ownerId, Post post, String name, LocalDateTime utcNow) {
		Bookmark bookmark = new Bookmark();
		bookmark.ownerId = ownerId;
		bookmark.postId = post.id();
		bookmark.postSlug = post.slug();
		bookmark.name = name;
		bookmark.createdAt = utcNow;
		return bookmark;
	}

	/** A new name, which counts as a modification. */
	public void rename(String name, LocalDateTime utcNow) {
		this.name = name;
		this.modifiedAt = utcNow;
	}

	/** A new name and order from arranging the display, which is not a modification. */
	public void arrange(String name, int sortOrder) {
		this.name = name;
		this.sortOrder = sortOrder;
	}

	public Long id() {
		return this.id;
	}

	public Long ownerId() {
		return this.ownerId;
	}

	public Long postId() {
		return this.postId;
	}

	public String postSlug() {
		return this.postSlug;
	}

	public String name() {
		return this.name;
	}

	public int sortOrder() {
		return this.sortOrder;
	}

	/** UTC. */
	public LocalDateTime createdAt() {
		return this.createdAt;
	}

	/** UTC; null until the bookmark is renamed. */
	public LocalDateTime modifiedAt() {
		return this.modifiedAt;
	}

}
