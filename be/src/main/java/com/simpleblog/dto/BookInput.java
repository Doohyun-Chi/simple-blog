package com.simpleblog.dto;

/** The request body of registering and editing a book in be/bookapispec.yaml. Any field may be null here; the service rejects what is not allowed. */
public record BookInput(String title, String author, String bookstoreUrl, String thumbnailUrl, String description) {
}
