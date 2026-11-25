package com.simpleblog.service;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

import com.simpleblog.component.BookPageFetcher;
import com.simpleblog.component.BookPageParser;
import com.simpleblog.component.BookStoreUrl;
import com.simpleblog.dto.BookInfo;
import com.simpleblog.dto.BookInput;
import com.simpleblog.dto.BookInsight;
import com.simpleblog.dto.BookList;
import com.simpleblog.dto.BookPreview;
import com.simpleblog.dto.BookReading;
import com.simpleblog.dto.BookReview;
import com.simpleblog.entity.Book;
import com.simpleblog.exception.BookNotFoundException;
import com.simpleblog.exception.InvalidBookException;
import com.simpleblog.exception.NotBookReaderException;
import com.simpleblog.repository.BookRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The bookshelf of be/bookapispec.yaml. Everyone reads it; only the reader of a book changes it. */
@Service
public class BookService {

	private static final int FIRST_YEAR = 1900;

	private static final int LAST_YEAR = 9999;

	private static final int TITLE_MAX = 300;

	private static final int AUTHOR_MAX = 200;

	private static final int DESCRIPTION_MAX = 2000;

	private static final int INSIGHT_MAX = 5000;

	/** The same rule as the slug of a post. */
	private static final Pattern SLUG = Pattern.compile("[A-Za-z0-9-]+");

	private final BookRepository bookRepository;

	private final UserService userService;

	private final BookStoreUrl storeUrl;

	private final BookPageFetcher pageFetcher;

	private final BookPageParser pageParser;

	private final Clock clock;

	private final int slugMaxLength;

	public BookService(BookRepository bookRepository, UserService userService, BookStoreUrl storeUrl, BookPageFetcher pageFetcher,
			BookPageParser pageParser, Clock clock, @Value("${simpleblog.post.slug.max-length}") int slugMaxLength) {
		this.bookRepository = bookRepository;
		this.userService = userService;
		this.storeUrl = storeUrl;
		this.pageFetcher = pageFetcher;
		this.pageParser = pageParser;
		this.clock = clock;
		this.slugMaxLength = slugMaxLength;
	}

	/** The books newest first, of the year when one is given, with the years that have books. */
	@Transactional(readOnly = true)
	public BookList listOfBooks(Integer year) {
		List<Book> books = year == null ? this.bookRepository.listOfBooks() : booksOf(year);
		return new BookList(this.bookRepository.listOfYears(), books.stream().map(BookInfo::from).toList());
	}

	private List<Book> booksOf(int year) {
		if (year < FIRST_YEAR || year > LAST_YEAR) {
			throw new InvalidBookException("year must be a four digit year");
		}
		return this.bookRepository.listOfBooksRegisteredBetween(LocalDate.of(year, 1, 1), LocalDate.of(year, 1, 1).plusYears(1));
	}

	/** What the store page of the URL says about a book. Nothing is stored. */
	public BookPreview previewBook(String url) {
		URI uri = this.storeUrl.publicUri(url);
		return this.pageParser.preview(this.pageFetcher.page(uri), uri);
	}

	@Transactional(readOnly = true)
	public BookInfo book(long id) {
		return BookInfo.from(found(id));
	}

	/** Registers an unread book of the signed-in user, dated today. */
	@Transactional
	public BookInfo createBook(BookInput input) {
		requireValid(input);
		Book book = Book.registeredBy(this.userService.currentWriterId(), today());
		book.revise(input.title().trim(), blankToNull(input.author()), input.bookstoreUrl().trim(), blankToNull(input.thumbnailUrl()),
				blankToNull(input.description()));
		return BookInfo.from(this.bookRepository.save(book));
	}

	@Transactional
	public BookInfo updateBook(long id, BookInput input) {
		Book book = ownBook(id);
		requireValid(input);
		book.revise(input.title().trim(), blankToNull(input.author()), input.bookstoreUrl().trim(), blankToNull(input.thumbnailUrl()),
				blankToNull(input.description()));
		return BookInfo.from(book);
	}

	@Transactional
	public void deleteBook(long id) {
		this.bookRepository.delete(ownBook(id));
	}

	/** Sets both reading dates as sent: a start date alone starts the book, a complete date after it finishes it. */
	@Transactional
	public BookInfo checkInBook(long id, BookReading reading) {
		Book book = ownBook(id);
		requireValid(reading);
		book.checkIn(reading.readingStartDate(), reading.readingCompleteDate());
		return BookInfo.from(book);
	}

	@Transactional
	public BookInfo saveBookInsight(long id, BookInsight body) {
		Book book = ownBook(id);
		if (body == null || body.insight() == null || length(body.insight()) > INSIGHT_MAX) {
			throw new InvalidBookException("insight is required and limited to " + INSIGHT_MAX + " characters");
		}
		book.noteInsight(blankToNull(body.insight()));
		return BookInfo.from(book);
	}

	@Transactional
	public BookInfo linkBookReview(long id, BookReview body) {
		Book book = ownBook(id);
		String slug = body == null ? null : body.trackbackSlug();
		if (slug != null && (!SLUG.matcher(slug).matches() || slug.length() > this.slugMaxLength)) {
			throw new InvalidBookException("trackbackSlug must be English letters, digits and '-', at most " + this.slugMaxLength);
		}
		book.linkReview(slug);
		return BookInfo.from(book);
	}

	private Book found(long id) {
		return this.bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
	}

	/** The book of the id, when the user of the current request is its reader. */
	private Book ownBook(long id) {
		Book book = found(id);
		if (!book.readBy(this.userService.currentViewerId())) {
			throw new NotBookReaderException(id);
		}
		return book;
	}

	private LocalDate today() {
		return LocalDate.now(this.clock);
	}

	private void requireValid(BookInput input) {
		if (input == null || input.title() == null || input.title().isBlank() || length(input.title()) > TITLE_MAX) {
			throw new InvalidBookException("title is required and limited to " + TITLE_MAX + " characters");
		}
		requireLength("author", input.author(), AUTHOR_MAX);
		requireLength("description", input.description(), DESCRIPTION_MAX);
		requireWebUrl("bookstoreUrl", input.bookstoreUrl(), true);
		requireWebUrl("thumbnailUrl", input.thumbnailUrl(), false);
	}

	private void requireWebUrl(String field, String url, boolean required) {
		boolean absent = url == null || url.isBlank();
		if (absent ? required : (url.length() > BookStoreUrl.MAX_LENGTH || !BookStoreUrl.isWebUrl(url.trim()))) {
			throw new InvalidBookException(field + " must be an http or https URL of at most " + BookStoreUrl.MAX_LENGTH + " characters");
		}
	}

	private void requireLength(String field, String value, int max) {
		if (value != null && length(value) > max) {
			throw new InvalidBookException(field + " is limited to " + max + " characters");
		}
	}

	private void requireValid(BookReading reading) {
		LocalDate start = reading == null ? null : reading.readingStartDate();
		LocalDate complete = reading == null ? null : reading.readingCompleteDate();
		if (complete != null && (start == null || complete.isBefore(start))) {
			throw new InvalidBookException("readingCompleteDate needs a readingStartDate that is not after it");
		}
		requireNotInFuture(start);
		requireNotInFuture(complete);
	}

	private void requireNotInFuture(LocalDate date) {
		if (date != null && date.isAfter(today())) {
			throw new InvalidBookException("a reading date cannot be in the future");
		}
	}

	private int length(String text) {
		return text.codePointCount(0, text.length());
	}

	private String blankToNull(String text) {
		return text == null || text.isBlank() ? null : text.trim();
	}

}
