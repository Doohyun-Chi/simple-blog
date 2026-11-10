package com.simpleblog.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Read-only view of the WordPress {@code wp_posts} table. Pages are rows with {@code post_type = 'page'} and posts
 * are rows with {@code post_type = 'post'}. {@code post_date} and {@code post_modified} are in the site's local time;
 * the {@code *_gmt} columns hold the same moments in UTC.
 */
@Entity
@Immutable
@Table(name = "wp_posts")
public class Post {

	@Id
	@Column(name = "ID")
	private Long id;

	@Column(name = "post_title")
	private String title;

	@Column(name = "post_content")
	private String content;

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
