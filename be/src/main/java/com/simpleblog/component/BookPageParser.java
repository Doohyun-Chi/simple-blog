package com.simpleblog.component;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.simpleblog.dto.BookPreview;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Reads what a store page says about a book from its {@code <meta>} tags: Open Graph ({@code og:title}, {@code og:description},
 * {@code og:image}) first, then the plain {@code author} and {@code description} metas and the {@code <title>}. A value is cut to
 * the limit of the field it is stored in, so the writer can register it as it is.
 */
@Component
public class BookPageParser {

	private static final Pattern META = Pattern.compile("<meta\\s[^>]*>", Pattern.CASE_INSENSITIVE);

	private static final Pattern ATTRIBUTE = Pattern.compile("([a-zA-Z:-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");

	private static final Pattern TITLE = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private static final String YES24_SUFFIX = " - 예스24";

	static final int TITLE_MAX = 300;

	static final int AUTHOR_MAX = 200;

	static final int DESCRIPTION_MAX = 2000;

	public BookPreview preview(String html, URI pageUrl) {
		Map<String, String> metas = metasOf(html);
		String title = first(metas, Meta.OG_TITLE);
		if (title == null) {
			Matcher matcher = TITLE.matcher(html);
			title = matcher.find() ? text(matcher.group(1)) : null;
		}
		String author = first(metas, Meta.AUTHOR, Meta.BOOK_AUTHOR, Meta.OG_BOOK_AUTHOR);
		BookPreview general = new BookPreview(cut(cleanTitle(title, pageUrl), TITLE_MAX),
				cut(author != null && author.startsWith("http") ? null : author, AUTHOR_MAX),
				cut(first(metas, Meta.OG_DESCRIPTION, Meta.DESCRIPTION), DESCRIPTION_MAX), thumbnailOf(first(metas, Meta.OG_IMAGE), pageUrl));
		return AmazonBookPage.isAmazon(pageUrl) ? AmazonBookPage.refine(html, pageUrl, general) : general;
	}

	/** Yes24 titles its pages `title | author | publisher - 예스24`; only the title is kept. */
	private String cleanTitle(String title, URI pageUrl) {
		if (title == null || pageUrl.getHost() == null || !pageUrl.getHost().toLowerCase(Locale.ROOT).endsWith("yes24.com")) {
			return title;
		}
		String text = text(title);
		String withoutSite = text.endsWith(YES24_SUFFIX) ? text.substring(0, text.length() - YES24_SUFFIX.length()) : text;
		int bar = withoutSite.indexOf(" | ");
		return bar > 0 ? withoutSite.substring(0, bar) : withoutSite;
	}

	private Map<String, String> metasOf(String html) {
		Map<String, String> metas = new HashMap<>();
		Matcher tags = META.matcher(html);
		while (tags.find()) {
			Map<String, String> attributes = attributesOf(tags.group());
			String key = attributes.getOrDefault("property", attributes.get("name"));
			String content = attributes.get("content");
			if (key != null && content != null) {
				metas.putIfAbsent(key.toLowerCase(Locale.ROOT), content);
			}
		}
		return metas;
	}

	private Map<String, String> attributesOf(String tag) {
		Map<String, String> attributes = new HashMap<>();
		Matcher matcher = ATTRIBUTE.matcher(tag);
		while (matcher.find()) {
			attributes.putIfAbsent(matcher.group(1).toLowerCase(Locale.ROOT), matcher.group(2) != null ? matcher.group(2) : matcher.group(3));
		}
		return attributes;
	}

	private String first(Map<String, String> metas, String... keys) {
		for (String key : keys) {
			String value = metas.get(key);
			if (value != null && !text(value).isEmpty()) {
				return value;
			}
		}
		return null;
	}

	/** The image URL resolved against the page, when it is an http or https one that fits the field it is stored in; the page may give a relative path. */
	static String thumbnailOf(String image, URI pageUrl) {
		if (image == null || image.isBlank()) {
			return null;
		}
		try {
			String resolved = pageUrl.resolve(text(image)).toString();
			return resolved.length() <= BookStoreUrl.MAX_LENGTH && BookStoreUrl.isWebUrl(resolved) ? resolved : null;
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

	static String cut(String value, int max) {
		String text = value == null ? "" : text(value);
		if (text.isEmpty()) {
			return null;
		}
		return text.codePointCount(0, text.length()) <= max ? text : text.substring(0, text.offsetByCodePoints(0, max));
	}

	/** The text with its HTML entities decoded, and with the white space trimmed and collapsed. */
	static String text(String raw) {
		return HtmlUtils.htmlUnescape(raw).replaceAll("\\s+", " ").trim();
	}

	/** The names of the metas that are read; a meta's name is its {@code property} (Open Graph) or {@code name} attribute. */
	private static final class Meta {

		static final String OG_TITLE = "og:title";

		static final String OG_DESCRIPTION = "og:description";

		static final String OG_IMAGE = "og:image";

		static final String OG_BOOK_AUTHOR = "og:book:author";

		static final String BOOK_AUTHOR = "book:author";

		static final String AUTHOR = "author";

		static final String DESCRIPTION = "description";

		private Meta() {
		}

	}

}
