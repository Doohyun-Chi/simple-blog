package com.simpleblog.component;

/** Text for the XML documents the server writes (the feed and the sitemap). */
public final class XmlText {

	private XmlText() {
	}

	/** Only the five characters XML predefines; HtmlUtils would write HTML-only entities such as &eacute; that XML parsers reject. */
	public static String escape(String text) {
		StringBuilder escaped = new StringBuilder(text.length());
		text.codePoints().filter(XmlText::isXmlChar).forEach((point) -> escaped.append(escaped(point)));
		return escaped.toString();
	}

	private static String escaped(int point) {
		return switch (point) {
			case '&' -> "&amp;";
			case '<' -> "&lt;";
			case '>' -> "&gt;";
			case '"' -> "&quot;";
			case '\'' -> "&apos;";
			default -> Character.toString(point);
		};
	}

	/** The characters XML 1.0 allows; the others (most control characters) would make the whole feed unparsable. */
	private static boolean isXmlChar(int point) {
		return point == 0x9 || point == 0xA || point == 0xD || (point >= 0x20 && point <= 0xD7FF)
				|| (point >= 0xE000 && point <= 0xFFFD) || (point >= 0x10000 && point <= 0x10FFFF);
	}

}
