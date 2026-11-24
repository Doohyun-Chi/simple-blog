package com.simpleblog.dto;

/** What a store page gives about a book; a field the page does not give is null. */
public record BookPreview(String title, String author, String description, String thumbnailUrl) {
}
