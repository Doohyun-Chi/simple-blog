package com.simpleblog.dto;

import java.time.LocalDateTime;

/** A published post as the feed query reads it: the stored slug and the UTC times of the table. */
public record FeedRow(String title, String slug, LocalDateTime createdAt, LocalDateTime modifiedAt) {
}
