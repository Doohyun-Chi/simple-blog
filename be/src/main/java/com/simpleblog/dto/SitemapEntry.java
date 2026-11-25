package com.simpleblog.dto;

import java.time.OffsetDateTime;

/** One post of the sitemap. {@code path} is the post view path ({@code /post/{slug}}); the origin is added when rendering. */
public record SitemapEntry(String path, OffsetDateTime lastModified) {
}
