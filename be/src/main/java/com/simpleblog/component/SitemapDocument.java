package com.simpleblog.component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.simpleblog.dto.Sitemap;
import com.simpleblog.dto.SitemapEntry;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Writes the sitemap document (https://www.sitemaps.org/protocol.html): the home page, then each published post with its last
 * modification. The document is the template templates/sitemap.xml and each post the template templates/sitemap-url.xml;
 * their {{holders}} are filled with XML-escaped values.
 */
@Component
public class SitemapDocument {

	private static final Pattern HOLDER = Pattern.compile("\\{\\{(\\w+)}}");

	private final String sitemapTemplate = read("templates/sitemap.xml");
	private final String urlTemplate = read("templates/sitemap-url.xml");

	/** {@code origin} is the frontend's origin, where the posts are read. */
	public String render(Sitemap sitemap, String origin) {
		StringBuilder urls = new StringBuilder();
		sitemap.posts().forEach((post) -> urls.append(renderUrl(post, origin)));
		return fill(this.sitemapTemplate, Map.of("origin", XmlText.escape(origin), "urls", urls.toString()));
	}

	private String renderUrl(SitemapEntry post, String origin) {
		return fill(this.urlTemplate, Map.of("url", XmlText.escape(origin + post.path()),
				"lastModified", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(post.lastModified().withNano(0))));
	}

	/** One pass over the template, so a holder written inside a value is never filled in again. */
	private static String fill(String template, Map<String, String> values) {
		return HOLDER.matcher(template).replaceAll((holder) -> Matcher.quoteReplacement(values.get(holder.group(1))));
	}

	private static String read(String path) {
		try {
			return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("The sitemap template " + path + " cannot be read", ex);
		}
	}

}
