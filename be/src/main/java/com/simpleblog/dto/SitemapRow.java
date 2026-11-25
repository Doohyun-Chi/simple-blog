package com.simpleblog.dto;

import java.time.LocalDateTime;

/** A published post as the sitemap query reads it: the stored slug and the UTC modification time of the table. */
public record SitemapRow(String slug, LocalDateTime modifiedAt) {
}
