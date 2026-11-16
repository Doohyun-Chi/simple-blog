package com.simpleblog.dto;

import java.time.LocalDateTime;

/** One entry of the post list in be/apispec.yaml. {@code date} is wp_posts.post_date, the site's local time. */
public record PostSummary(Long id, String title, String slug, LocalDateTime date) {
}
