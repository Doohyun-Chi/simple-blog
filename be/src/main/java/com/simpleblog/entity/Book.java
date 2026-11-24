package com.simpleblog.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The {@code TB_BOOK} table of DBSCHEMA.md: a book of the bookshelf. The dates carry no time. A book is registered unread; its
 * reading dates, insight and review slug are set later, each on its own.
 */
@Entity
@Table(name = "TB_BOOK")
public class Book {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

	@Column(name = "TITLE")
	private String title;

	@Column(name = "AUTHOR")
	private String author;

	@Column(name = "BOOKSTORE_URL")
	private String bookstoreUrl;

	@Column(name = "THUMBNAIL_URL")
	private String thumbnailUrl;

	@Column(name = "DESCRIPTION")
	private String description;

	@Column(name = "INSIGHT")
	private String insight;

	@Column(name = "TRACKBACK_SLUG")
	private String trackbackSlug;

	@Column(name = "REGISTERED_DATE")
	private LocalDate registeredDate;

	@Column(name = "READING_START_DATE")
	private LocalDate readingStartDate;

	@Column(name = "READING_COMPLETE_DATE")
	private LocalDate readingCompleteDate;

	@Column(name = "READER_ID")
	private Long readerId;

	protected Book() {
	}

	private Book(long readerId, LocalDate registeredDate) {
		this.readerId = readerId;
		this.registeredDate = registeredDate;
	}

	/** An unread book of the reader, registered on the given day, with the details still to be set by {@link #revise}. */
	public static Book registeredBy(long readerId, LocalDate registeredDate) {
		return new Book(readerId, registeredDate);
	}

	/** Replaces what the writer edits at registration; the dates, insight and review stay. */
	public void revise(String title, String author, String bookstoreUrl, String thumbnailUrl, String description) {
		this.title = title;
		this.author = author;
		this.bookstoreUrl = bookstoreUrl;
		this.thumbnailUrl = thumbnailUrl;
		this.description = description;
	}

	public void checkIn(LocalDate start, LocalDate complete) {
		this.readingStartDate = start;
		this.readingCompleteDate = complete;
	}

	public void noteInsight(String insight) {
		this.insight = insight;
	}

	public void linkReview(String trackbackSlug) {
		this.trackbackSlug = trackbackSlug;
	}

	public boolean readBy(long userId) {
		return this.readerId != null && this.readerId == userId;
	}

	public Long id() {
		return this.id;
	}

	public String title() {
		return this.title;
	}

	public String author() {
		return this.author;
	}

	public String bookstoreUrl() {
		return this.bookstoreUrl;
	}

	public String thumbnailUrl() {
		return this.thumbnailUrl;
	}

	public String description() {
		return this.description;
	}

	public String insight() {
		return this.insight;
	}

	public String trackbackSlug() {
		return this.trackbackSlug;
	}

	public LocalDate registeredDate() {
		return this.registeredDate;
	}

	public LocalDate readingStartDate() {
		return this.readingStartDate;
	}

	public LocalDate readingCompleteDate() {
		return this.readingCompleteDate;
	}

}
