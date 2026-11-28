package com.simpleblog.dto;

/**
 * An image upload as in be/apispec.yaml ({@code MediaUpload}). {@code slug} is null when the slug is to be made of the file
 * name; {@code replace} only matters with a slug. {@code source} and {@code sourceType} are null when the image comes
 * without a source.
 */
public record ImageUpload(String fileName, byte[] content, String provider, String slug, boolean replace, String source,
		String sourceType) {
}
