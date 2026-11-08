package com.simpleblog.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Read-only view of the WordPress {@code wp_posts} table. Pages are rows with {@code post_type = 'page'}.
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

	public String slug() {
		return this.slug;
	}

	public Long parentId() {
		return this.parentId;
	}

	public int menuOrder() {
		return this.menuOrder;
	}

	public LocalDateTime publishedAtGmt() {
		return this.publishedAtGmt;
	}

	public LocalDateTime modifiedAtGmt() {
		return this.modifiedAtGmt;
	}

}
