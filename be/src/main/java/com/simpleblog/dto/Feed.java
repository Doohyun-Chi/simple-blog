package com.simpleblog.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** The newest published posts, newest first, and when the feed last changed. */
public record Feed(List<FeedEntry> entries, OffsetDateTime updated) {
}
