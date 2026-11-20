package com.simpleblog.dto;

/** What a link preview shows for a post or a page. {@code image} and {@code url} are absolute URLs. */
public record ShareTags(String title, String description, String image, String url) {
}
