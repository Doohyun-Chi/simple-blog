package com.simpleblog.dto;

import org.springframework.core.io.Resource;

/** The stored bytes of an internal media item, the mime type to serve them with, and the stored file name, which changes with the bytes. */
public record MediaFile(Resource resource, String mimeType, String stored) {
}
