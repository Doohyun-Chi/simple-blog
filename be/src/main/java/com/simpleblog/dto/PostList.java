package com.simpleblog.dto;

import java.util.List;

/** {@code hasNext}: a match follows the last post of the list. */
public record PostList(List<PostSummary> posts, boolean hasNext) {
}
