package com.simpleblog.component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/** What a link preview takes from the content of a post: a short plain-text description and the first image. */
@Component
public class PostExcerpt {

	static final int DESCRIPTION_LENGTH = 200;

	private static final String ELLIPSIS = "\u2026";

	private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

	private static final Pattern IMAGE = Pattern.compile("!\\[[^\\]]*]\\([^)]*\\)");

	private static final Pattern LINK = Pattern.compile("\\[([^\\]]*)]\\([^)]*\\)");

	private static final Pattern TAG = Pattern.compile("<[^>]*>");

	private static final Pattern MARKDOWN = Pattern.compile("(?m)^\\s{0,3}(#{1,6}|>)\\s*|\\*\\*|__|~~|`");

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	/** A Markdown image {@code ![alt](src)} or an HTML {@code <img src>}, whichever comes first. The source is group 1 or 2. */
	private static final Pattern FIRST_IMAGE = Pattern.compile(
			"!\\[[^\\]]*]\\(\\s*<?([^)\\s>]+)|<img\\b[^>]*?\\bsrc\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

	/**
	 * The first 200 characters of the content as plain text, cut before a word so that none is broken, with an ellipsis
	 * when anything was cut. A text that fits is returned whole. A first word longer than the limit is cut at the limit.
	 */
	public String descriptionOf(String content) {
		String text = plainTextOf(content);
		if (text.codePointCount(0, text.length()) <= DESCRIPTION_LENGTH) {
			return text;
		}
		int limit = text.offsetByCodePoints(0, DESCRIPTION_LENGTH);
		int space = Character.isWhitespace(text.charAt(limit)) ? limit : text.lastIndexOf(' ', limit);
		return text.substring(0, space > 0 ? space : limit).strip() + ELLIPSIS;
	}

	/**
	 * The URL of the first image in the content, absolute: a relative source is resolved against {@code origin}, an
	 * absolute one is kept. An image that is not a web address, such as an inline data URI, is skipped.
	 */
	public Optional<String> firstImageOf(String content, String origin) {
		Matcher image = FIRST_IMAGE.matcher(content == null ? "" : content);
		while (image.find()) {
			String source = HtmlUtils.htmlUnescape((image.group(1) != null ? image.group(1) : image.group(2)).strip());
			Optional<String> absolute = absoluteOf(source, origin);
			if (absolute.isPresent()) {
				return absolute;
			}
		}
		return Optional.empty();
	}

	private Optional<String> absoluteOf(String source, String origin) {
		String lower = source.toLowerCase();
		if (lower.startsWith("https://") || lower.startsWith("http://")) {
			return Optional.of(source);
		}
		if (source.startsWith("//")) {
			return Optional.of("https:" + source);
		}
		if (source.isEmpty() || lower.startsWith("data:") || lower.startsWith("javascript:") || source.contains(":")) {
			return Optional.empty();
		}
		return Optional.of(origin + (source.startsWith("/") ? "" : "/") + source);
	}

	private String plainTextOf(String content) {
		String text = COMMENT.matcher(content == null ? "" : content).replaceAll(" ");
		text = IMAGE.matcher(text).replaceAll(" ");
		text = LINK.matcher(text).replaceAll("$1");
		text = TAG.matcher(text).replaceAll(" ");
		text = MARKDOWN.matcher(text).replaceAll("");
		return WHITESPACE.matcher(HtmlUtils.htmlUnescape(text)).replaceAll(" ").strip();
	}

}
