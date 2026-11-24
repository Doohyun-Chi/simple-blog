package com.simpleblog.component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.simpleblog.dto.BookPreview;

/**
 * What an Amazon product page says about a book. Its metas are no help (no Open Graph; the title is the long store title and the
 * description is shipping text), so the title, the authors and the cover are read from the page's own elements.
 */
final class AmazonBookPage {

	private static final Pattern PRODUCT_TITLE = Pattern.compile("id=\"productTitle\"[^>]*>(.*?)</span>", Pattern.DOTALL);

	private static final Pattern AUTHOR = Pattern.compile("class=\"author[^\"]*\"[^>]*>\\s*<a[^>]*>([^<]+)</a>");

	private static final Pattern COVER = Pattern.compile("<img\\b[^>]*\\bid=\"landingImage\"[^>]*>");

	private static final Pattern IMAGE_URL = Pattern.compile("https://[^\\s\"&;]+\\.(?:jpg|png|webp)");

	private static final int BYLINE_LENGTH = 3000;

	private AmazonBookPage() {
	}

	static boolean isAmazon(URI page) {
		return page.getHost() != null && page.getHost().toLowerCase(Locale.ROOT).contains("amazon.");
	}

	/** The general preview with the title, author and cover of the page's elements where it has them; the shipping description is dropped. */
	static BookPreview refine(String html, URI page, BookPreview general) {
		String title = titleOf(html);
		String authors = authorsOf(html);
		String cover = coverOf(html);
		return new BookPreview(title != null ? title : general.title(), authors != null ? authors : general.author(), null,
				cover != null ? cover : general.thumbnailUrl());
	}

	private static String titleOf(String html) {
		Matcher matcher = PRODUCT_TITLE.matcher(html);
		return matcher.find() ? BookPageParser.cut(BookPageParser.text(matcher.group(1)), BookPageParser.TITLE_MAX) : null;
	}

	private static String authorsOf(String html) {
		int byline = html.indexOf("id=\"bylineInfo\"");
		if (byline < 0) {
			return null;
		}
		Matcher matcher = AUTHOR.matcher(html.substring(byline, Math.min(html.length(), byline + BYLINE_LENGTH)));
		List<String> names = new ArrayList<>();
		while (matcher.find()) {
			names.add(BookPageParser.text(matcher.group(1)));
		}
		return BookPageParser.cut(names.isEmpty() ? null : String.join(", ", names), BookPageParser.AUTHOR_MAX);
	}

	/** The largest of the cover sizes the page lists, which are in ascending order. */
	private static String coverOf(String html) {
		Matcher tag = COVER.matcher(html);
		if (!tag.find()) {
			return null;
		}
		Matcher urls = IMAGE_URL.matcher(BookPageParser.text(tag.group()));
		String last = null;
		while (urls.find()) {
			last = urls.group();
		}
		return last;
	}

}
