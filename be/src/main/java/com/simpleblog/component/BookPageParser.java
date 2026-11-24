package com.simpleblog.component;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.simpleblog.dto.BookPreview;
import org.springframework.stereotype.Component;

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

	private static final Pattern ENTITY = Pattern.compile("&(#x[0-9a-fA-F]+|#\\d+|amp|lt|gt|quot|apos);");

	private static final String YES24_SUFFIX = " - 예스24";

	static final int TITLE_MAX = 300;

	static final int AUTHOR_MAX = 200;

	static final int DESCRIPTION_MAX = 2000;

	public BookPreview preview(String html, URI pageUrl) {
		Map<String, String> metas = metasOf(html);
		String title = first(metas, "og:title");
		if (title == null) {
			Matcher matcher = TITLE.matcher(html);
			title = matcher.find() ? text(matcher.group(1)) : null;
		}
		String author = first(metas, "author", "book:author", "og:book:author");
		BookPreview general = new BookPreview(cut(cleanTitle(title, pageUrl), TITLE_MAX),
				cut(author != null && author.startsWith("http") ? null : author, AUTHOR_MAX),
				cut(first(metas, "og:description", "description"), DESCRIPTION_MAX), thumbnailOf(first(metas, "og:image"), pageUrl));
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

	/** The image URL resolved against the page, when it is an http or https one; the page may give a relative path. */
	static String thumbnailOf(String image, URI pageUrl) {
		if (image == null || image.isBlank()) {
			return null;
		}
		try {
			URI resolved = pageUrl.resolve(text(image));
			return "http".equalsIgnoreCase(resolved.getScheme()) || "https".equalsIgnoreCase(resolved.getScheme()) ? resolved.toString() : null;
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
		Matcher matcher = ENTITY.matcher(raw);
		StringBuilder decoded = new StringBuilder();
		while (matcher.find()) {
			matcher.appendReplacement(decoded, Matcher.quoteReplacement(entity(matcher.group(1))));
		}
		matcher.appendTail(decoded);
		return decoded.toString().replaceAll("\\s+", " ").trim();
	}

	private static String entity(String name) {
		return switch (name) {
			case "amp" -> "&";
			case "lt" -> "<";
			case "gt" -> ">";
			case "quot" -> "\"";
			case "apos" -> "'";
			default -> numeric(name);
		};
	}

	/** The character of {@code #123} or {@code #x7B}; nothing for a number that is no character. */
	private static String numeric(String name) {
		try {
			int codePoint = name.startsWith("#x") ? Integer.parseInt(name.substring(2), 16) : Integer.parseInt(name.substring(1));
			return new String(Character.toChars(codePoint));
		}
		catch (IllegalArgumentException ex) {
			return "";
		}
	}

}
