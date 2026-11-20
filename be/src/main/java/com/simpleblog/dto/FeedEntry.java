package com.simpleblog.dto;

import java.time.OffsetDateTime;

/** One entry of the feed. {@code path} is the post view path ({@code /post/{slug}}); the origin is added when rendering. */
public record FeedEntry(String title, String path, OffsetDateTime published, OffsetDateTime updated) {
}
