package com.simpleblog.dto;

import java.util.List;

/** The published posts a sitemap lists, newest modification first. */
public record Sitemap(List<SitemapEntry> posts) {
}
