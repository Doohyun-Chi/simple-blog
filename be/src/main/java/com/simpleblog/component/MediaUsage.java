package com.simpleblog.component;

import java.util.regex.Pattern;

import com.simpleblog.repository.PostRepository;
import org.springframework.stereotype.Component;

/**
 * Whether a post or page, of any status, shows an internal image: its content holds the image's file URL. The URL must
 * end right after the slug, so one slug that begins another does not count as it.
 */
@Component
public class MediaUsage {

	/** The path of an internal image's file in be/apispec.yaml, before its slug. */
	private static final String FILE_PATH = "/public/api/v1/media/";

	private final PostRepository postRepository;

	private final LikePattern likePattern;

	public MediaUsage(PostRepository postRepository, LikePattern likePattern) {
		this.postRepository = postRepository;
		this.likePattern = likePattern;
	}

	public boolean inUse(String slug) {
		Pattern fileUrl = Pattern.compile(Pattern.quote(FILE_PATH + slug) + "(?![A-Za-z0-9_-])");
		return this.postRepository.listOfContentsContaining(this.likePattern.containing(FILE_PATH + slug))
			.stream()
			.anyMatch((content) -> fileUrl.matcher(content).find());
	}

}
