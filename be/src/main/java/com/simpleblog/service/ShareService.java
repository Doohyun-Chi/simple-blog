package com.simpleblog.service;

import com.simpleblog.component.PostExcerpt;
import com.simpleblog.component.SlugEncoder;
import com.simpleblog.component.ViewLinks;
import com.simpleblog.dto.ShareTags;
import com.simpleblog.entity.Post;
import com.simpleblog.exception.PostNotFoundException;
import com.simpleblog.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The tags of a link preview for a published post or page. */
@Service
public class ShareService {

	static final String FALLBACK_IMAGE = "/realizing-future.jpg";

	private final PostRepository postRepository;

	private final SlugEncoder slugEncoder;

	private final PostExcerpt excerpt;

	private final ViewLinks viewLinks;

	public ShareService(PostRepository postRepository, SlugEncoder slugEncoder, PostExcerpt excerpt, ViewLinks viewLinks) {
		this.postRepository = postRepository;
		this.slugEncoder = slugEncoder;
		this.excerpt = excerpt;
		this.viewLinks = viewLinks;
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
		String frontend = this.viewLinks.frontendOrigin(backendOrigin);
		String image = this.excerpt.firstImageOf(post.content(), backendOrigin).orElse(frontend + FALLBACK_IMAGE);
		String url = frontend + this.viewLinks.pathOf(post.type(), post.slug());
		return new ShareTags(post.title(), this.excerpt.descriptionOf(post.content()), image, url);
	}

	/** Only a published post or page has a share page, so a draft or a private post never shows its title or text. */
	private static boolean shareable(Post post) {
		return "publish".equals(post.status()) && ("post".equals(post.type()) || "page".equals(post.type()));
	}

}
