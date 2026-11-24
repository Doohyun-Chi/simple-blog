package com.simpleblog.dto;

import java.time.LocalDate;

import com.simpleblog.entity.Book;

/** A book as in be/bookapispec.yaml ({@code Book}). The reader's id is not part of it. */
public record BookInfo(long id, String title, String author, String bookstoreUrl, String thumbnailUrl, String description, String insight,
		String trackbackSlug, LocalDate registeredDate, LocalDate readingStartDate, LocalDate readingCompleteDate) {

	public static BookInfo from(Book book) {
		return new BookInfo(book.id(), book.title(), book.author(), book.bookstoreUrl(), book.thumbnailUrl(), book.description(),
				book.insight(), book.trackbackSlug(), book.registeredDate(), book.readingStartDate(), book.readingCompleteDate());
	}

}
