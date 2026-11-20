package com.simpleblog.component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.simpleblog.dto.Feed;
import com.simpleblog.dto.FeedEntry;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Writes the Atom 1.0 document of the feed (RFC 4287): titles and links only. The document is the template
 * templates/feed.xml and each post the template templates/feed-entry.xml; their {{holders}} are filled with XML-escaped values.
 */
@Component
public class FeedDocument {

	static final String BLOG_NAME = "Dreaming for the Future";

	private static final Pattern HOLDER = Pattern.compile("\\{\\{(\\w+)}}");

	private final String feedTemplate = read("templates/feed.xml");
	private final String entryTemplate = read("templates/feed-entry.xml");

	/** {@code origin} is the frontend's origin, where the posts are read; {@code selfUrl} is the address of the feed itself. */
	public String render(Feed feed, String origin, String selfUrl) {
		StringBuilder entries = new StringBuilder();
		feed.entries().forEach((entry) -> entries.append(renderEntry(entry, origin)));
		return fill(this.feedTemplate, Map.of("name", escape(BLOG_NAME), 
		                                      "origin", escape(origin),
											  "selfUrl", escape(selfUrl),
											  "updated", time(feed.updated()), 
											  "entries", entries.toString()));
	}

	private String renderEntry(FeedEntry entry, String origin) {
		return fill(this.entryTemplate, Map.of("name", escape(BLOG_NAME), 
		                                       "url", escape(origin + entry.path()),
											   "title", escape(entry.title()),
											   "published", time(entry.published()),
											   "updated", time(entry.updated())));
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
			throw new UncheckedIOException("The feed template " + path + " cannot be read", ex);
		}
	}

	private static String time(OffsetDateTime time) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time.withNano(0));
	}

	private static String escape(String text) {
		return HtmlUtils.htmlEscape(text);
	}

}
