package com.simpleblog.component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.simpleblog.dto.ShareTags;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * The HTML page link-preview crawlers read: the tags in the head and, for a person who lands on it, a link to the post
 * view that a script follows at once. The page is the template templates/share.html, whose {{title}}, {{description}},
 * {{image}} and {{url}} holders are filled with the HTML-escaped values. There is no {@code og:url} in it: crawlers take it as
 * the canonical address and read the tags from there, which for a post view URL is the frontend's page without any.
 */
@Component
public class SharePage {

	private static final Pattern HOLDER = Pattern.compile("\\{\\{(\\w+)}}");
	private static final String TEMPLATE_PATH = "templates/share.html";

	private final String template;

	public SharePage() {
		try {
			this.template = new ClassPathResource(TEMPLATE_PATH).getContentAsString(StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("The share page template cannot be read", ex);
		}
	}

	public String render(ShareTags tags) {
		Map<String, String> values = Map.of("title", HtmlUtils.htmlEscape(tags.title()),
									"description", HtmlUtils.htmlEscape(tags.description()),
									"image", HtmlUtils.htmlEscape(tags.image()),
									"url", HtmlUtils.htmlEscape(tags.url()));
		// One pass over the template, so a holder written inside a value is never filled in again.
		return HOLDER.matcher(this.template).replaceAll((holder) -> Matcher.quoteReplacement(values.get(holder.group(1))));
	}

}
