package com.simpleblog.controller;

import com.simpleblog.dto.BookInfo;
import com.simpleblog.dto.BookInput;
import com.simpleblog.dto.BookInsight;
import com.simpleblog.dto.BookList;
import com.simpleblog.dto.BookPreview;
import com.simpleblog.dto.BookReading;
import com.simpleblog.dto.BookReview;
import com.simpleblog.service.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The bookshelf API of be/bookapispec.yaml. */
@RestController
@RequestMapping("/public/api/v1")
public class BookController {

	private final BookService bookService;

	public BookController(BookService bookService) {
		this.bookService = bookService;
	}

	@GetMapping("/books")
	public BookList listOfBooks(@RequestParam(required = false) Integer year) {
		return this.bookService.listOfBooks(year);
	}

	@GetMapping("/book-preview")
	public BookPreview previewBook(@RequestParam(required = false) String url) {
		return this.bookService.previewBook(url);
	}

	@PostMapping("/book")
	@ResponseStatus(HttpStatus.CREATED)
	public BookInfo createBook(@RequestBody BookInput input) {
		return this.bookService.createBook(input);
	}

	@GetMapping("/book/{id}")
	public BookInfo book(@PathVariable long id) {
		return this.bookService.book(id);
	}

	@PutMapping("/book/{id}")
	public BookInfo updateBook(@PathVariable long id, @RequestBody BookInput input) {
		return this.bookService.updateBook(id, input);
	}

	@DeleteMapping("/book/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteBook(@PathVariable long id) {
		this.bookService.deleteBook(id);
	}

	@PutMapping("/book/{id}/reading")
	public BookInfo checkInBook(@PathVariable long id, @RequestBody BookReading reading) {
		return this.bookService.checkInBook(id, reading);
	}

	@PutMapping("/book/{id}/insight")
	public BookInfo saveBookInsight(@PathVariable long id, @RequestBody BookInsight insight) {
		return this.bookService.saveBookInsight(id, insight);
	}

	@PutMapping("/book/{id}/review")
	public BookInfo linkBookReview(@PathVariable long id, @RequestBody BookReview review) {
		return this.bookService.linkBookReview(id, review);
	}

}
