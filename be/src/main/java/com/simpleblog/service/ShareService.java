package com.simpleblog.service;

import java.nio.charset.StandardCharsets;

import com.simpleblog.component.PostExcerpt;
import com.simpleblog.component.SlugEncoder;
import com.simpleblog.dto.ShareTags;
import com.simpleblog.entity.Post;
import com.simpleblog.exception.PostNotFoundException;
import com.simpleblog.repository.PostRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

/** The tags of a link preview for a published post or page. */
@Service
public class ShareService {

	static final String FALLBACK_IMAGE = "/realizing-future.jpg";

	private final PostRepository postRepository;

	private final SlugEncoder slugEncoder;

	private final PostExcerpt excerpt;

	private final String frontendUrl;

	public ShareService(PostRepository postRepository, SlugEncoder slugEncoder, PostExcerpt excerpt,
			@Value("${simpleblog.auth.frontend-url:}") String frontendUrl) {
		this.postRepository = postRepository;
		this.slugEncoder = slugEncoder;
		this.excerpt = excerpt;
		this.frontendUrl = frontendUrl.strip().replaceAll("/+$", "");
	}

	/**
	 * The tags for the row with this slug, as readers see it. {@code backendOrigin} is where this server is reached: it
	 * completes a relative image source, and stands in for the frontend when none is configured (same origin).
	 */
	@Transactional(readOnly = true)
	public ShareTags shareOfPost(String slug, String backendOrigin) {
		Post post = this.postRepository.post(this.slugEncoder.encode(slug))
			.filter(ShareService::shareable)
			.orElseThrow(() -> new PostNotFoundException(slug));
		String frontend = this.frontendUrl.isEmpty() ? backendOrigin : this.frontendUrl;
		String image = this.excerpt.firstImageOf(post.content(), backendOrigin).orElse(frontend + FALLBACK_IMAGE);
		return new ShareTags(post.title(), this.excerpt.descriptionOf(post.content()), image, frontend + viewPath(post));
	}

	/** Only a published post or page has a share page, so a draft or a private post never shows its title or text. */
	private static boolean shareable(Post post) {
		return "publish".equals(post.status()) && ("post".equals(post.type()) || "page".equals(post.type()));
	}

	/** Where readers open the row: pages at /page/{slug}, posts at /post/{slug}, the slug decoded once and then URL-encoded. */
	private static String viewPath(Post post) {
		return ("page".equals(post.type()) ? "/page/" : "/post/") + UriUtils.encodePathSegment(readerSlug(post.slug()), StandardCharsets.UTF_8);
	}

	private static String readerSlug(String storedSlug) {
		try {
			return UriUtils.decode(storedSlug, StandardCharsets.UTF_8);
		}
		catch (IllegalArgumentException ex) {
			return storedSlug;
		}
	}

}
