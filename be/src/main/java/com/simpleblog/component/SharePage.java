package com.simpleblog.component;

import com.simpleblog.dto.ShareTags;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/** The HTML page link-preview crawlers read: the tags in the head and, for a person who lands on it, a link to the post view. */
@Component
public class SharePage {

	public String render(ShareTags tags) {
		String title = HtmlUtils.htmlEscape(tags.title());
		String url = HtmlUtils.htmlEscape(tags.url());
		return String.join("\n", "<!doctype html>", "<html lang=\"ko\">", "<head>", "<meta charset=\"utf-8\" />",
				"<title>" + title + "</title>", property("og:title", tags.title()), property("og:description", tags.description()),
				property("og:image", tags.image()), property("og:url", tags.url()),
				"<meta name=\"twitter:card\" content=\"summary_large_image\" />", "</head>", "<body>",
				"<a href=\"" + url + "\">" + title + "</a>", "</body>", "</html>", "");
	}

	private String property(String name, String value) {
		return "<meta property=\"" + name + "\" content=\"" + HtmlUtils.htmlEscape(value) + "\" />";
	}

}
