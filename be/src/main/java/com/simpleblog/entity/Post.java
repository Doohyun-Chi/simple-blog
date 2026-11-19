package com.simpleblog.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * The {@code TB_POST} table, see be/DBSCHEMA.md. Pages are rows with {@code TYPE = 'page'} and posts are rows with
 * {@code TYPE = 'post'}. Both datetimes are UTC; readers get them converted to the site's local time where the API
 * says so.
 * <p>
 * Rows are only ever created as drafts of type post ({@link #draftOf}) and revised in title, slug and content
 * ({@link #revise}).
 */
@Entity
@Table(name = "TB_POST", uniqueConstraints = @UniqueConstraint(name = "IDX_TB_POST_SLUG", columnNames = "SLUG"))
public class Post {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

	@Column(name = "AUTHOR_ID")
	private Long authorId;

	@Column(name = "TITLE")
	private String title;

	@Column(name = "CONTENT")
	private String content;

	@Column(name = "SLUG")
	private String slug;

	@Column(name = "TYPE")
	private String type;

	@Column(name = "STATUS")
	private String status;

	@Column(name = "CREATED_DATETIME")
	private LocalDateTime createdAt;

	@Column(name = "MODIFIED_DATETIME")
	private LocalDateTime modifiedAt;

	protected Post() {
	}

	/** A new draft post. The slug is the stored form; see SlugEncoder. {@code utcNow} is both of its times. */
	public static Post draftOf(long authorId, String title, String slug, String content, LocalDateTime utcNow) {
		Post post = new Post();
		post.authorId = authorId;
		post.type = "post";
		post.status = "draft";
		post.createdAt = utcNow;
		post.revise(title, slug, content, utcNow);
		return post;
	}

	/** Sets the status, which the service has checked to be draft or publish. */
	public void moveTo(String status) {
		this.status = status;
	}

	/** Replaces title, slug and content and records the modification time. The status stays as it is. */
	public void revise(String title, String slug, String content, LocalDateTime utcNow) {
		this.title = title;
		this.slug = slug;
		this.content = content;
		this.modifiedAt = utcNow;
	}

	/** Whether the user with this id may update and delete the row: its author, and only a row of type post. */
	public boolean editableBy(long userId) {
		return this.authorId != null && this.authorId == userId;
	}

	public Long id() {
		return this.id;
	}

	public Long authorId() {
		return this.authorId;
	}

	public String title() {
		return this.title;
	}

	public String content() {
		return this.content;
	}

	public String slug() {
		return this.slug;
	}

	public String status() {
		return this.status;
	}

	/** UTC. */
	public LocalDateTime createdAt() {
		return this.createdAt;
	}

	/** UTC. */
	public LocalDateTime modifiedAt() {
		return this.modifiedAt;
	}

}
