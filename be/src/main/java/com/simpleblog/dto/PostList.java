package com.simpleblog.dto;

import java.util.List;

public record PostList(List<PostSummary> posts) {
}
