package com.simpleblog.dto;

import org.springframework.core.io.Resource;

/** The stored bytes of an internal media item and the mime type to serve them with. */
public record MediaFile(Resource resource, String mimeType) {
}
