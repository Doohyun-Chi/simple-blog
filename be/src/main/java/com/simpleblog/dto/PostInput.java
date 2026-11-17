package com.simpleblog.dto;

/**
 * The request body of creating and updating a post in be/apispec.yaml. {@code slug} is as readers see it, not
 * percent-encoded. Any field may be null here because the JSON may omit it; the service rejects what is not allowed.
 */
public record PostInput(String title, String slug, String content) {
}
