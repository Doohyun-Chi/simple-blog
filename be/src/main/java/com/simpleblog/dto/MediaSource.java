package com.simpleblog.dto;

import com.simpleblog.entity.SourceType;

/** The source of an image as text, as {@code MediaSource} in be/mcpapispec.yaml. */
public record MediaSource(String slug, SourceType sourceType, String source) {
}
