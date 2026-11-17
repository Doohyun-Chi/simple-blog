package com.simpleblog.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The WordPress {@code wp_posts} table. Pages are rows with {@code post_type = 'page'} and posts are rows with
 * {@code post_type = 'post'}. {@code post_date} and {@code post_modified} are in the site's local time; the
 * {@code *_gmt} columns hold the same moments in UTC.
 * <p>
 * Rows are only ever created as drafts of type post ({@link #draftOf}) and revised in title, slug and content
 * ({@link #revise}). The columns WordPress declares NOT NULL without a default (excerpt, to_ping, pinged and
 * content_filtered) are mapped so an insert can set them to an empty string.
 */
@Entity
@Table(name = "wp_posts")
public class Post {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

	@Column(name = "post_author")
	private Long authorId;

	@Column(name = "post_title")
	private String title;

	@Column(name = "post_content")
	private String content;

	@Column(name = "post_excerpt")
	private String excerpt;

	@Column(name = "to_ping")
	private String toPing;

	@Column(name = "pinged")
	private String pinged;

	@Column(name = "post_content_filtered")
	private String contentFiltered;

	@Column(name = "post_password")
	private String password;

	@Column(name = "post_name")
	private String slug;

	@Column(name = "post_type")
	private String type;

	@Column(name = "post_status")
	private String status;

	@Column(name = "post_parent")
	private Long parentId;

	@Column(name = "menu_order")
	private int menuOrder;

	@Column(name = "post_date")
	private LocalDateTime publishedAtLocal;

	@Column(name = "post_modified")
	private LocalDateTime modifiedAtLocal;

	@Column(name = "post_date_gmt")
	private LocalDateTime publishedAtGmt;

	@Column(name = "post_modified_gmt")
	private LocalDateTime modifiedAtGmt;

	protected Post() {
	}

	/** A new draft post. The slug is the stored form; see SlugEncoder. Both times are the creation moment. */
	public static Post draftOf(long authorId, String title, String slug, String content, LocalDateTime local,
			LocalDateTime gmt) {
		Post post = new Post();
		post.authorId = authorId;
		post.type = "post";
		post.status = "draft";
		post.password = "";
		post.parentId = 0L;
		post.menuOrder = 0;
		post.excerpt = "";
		post.toPing = "";
		post.pinged = "";
		post.contentFiltered = "";
		post.publishedAtLocal = local;
		post.publishedAtGmt = gmt;
		post.revise(title, slug, content, local, gmt);
		return post;
	}

	/** Replaces title, slug and content and records the modification time. The status stays as it is. */
	public void revise(String title, String slug, String content, LocalDateTime local, LocalDateTime gmt) {
		this.title = title;
		this.slug = slug;
		this.content = content;
		this.modifiedAtLocal = local;
		this.modifiedAtGmt = gmt;
	}

	public Long id() {
		return this.id;
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

	public Long parentId() {
		return this.parentId;
	}

	public int menuOrder() {
		return this.menuOrder;
	}

	public LocalDateTime publishedAtLocal() {
		return this.publishedAtLocal;
	}

	public LocalDateTime modifiedAtLocal() {
		return this.modifiedAtLocal;
	}

	public LocalDateTime publishedAtGmt() {
		return this.publishedAtGmt;
	}

	public LocalDateTime modifiedAtGmt() {
		return this.modifiedAtGmt;
	}

}
