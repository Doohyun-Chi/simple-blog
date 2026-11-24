package com.simpleblog.dto;

/** The request body of linking a review post to a book; a null slug unlinks it. */
public record BookReview(String trackbackSlug) {
}
