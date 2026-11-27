package com.simpleblog.component;

import java.util.HashMap;

/** Text for the XML documents the server writes (the feed and the sitemap). */
public final class XmlText {
	private final static HashMap<Integer, String> ESCAPED;

	static {
		ESCAPED = new HashMap<>();
		
		ESCAPED.put((int) '&', "&amp;");
		ESCAPED.put((int) '<', "&lt;");
		ESCAPED.put((int) '>', "&gt;");
		ESCAPED.put((int) '"', "&quot;");
		ESCAPED.put((int) '\'', "&apos;");
	}

	private XmlText() {
	}

	/** Only the five characters XML predefines; HtmlUtils would write HTML-only entities such as &eacute; that XML parsers reject. */
	public static String escape(String text) {
		StringBuilder escaped = new StringBuilder(text.length());
		text.codePoints().filter(XmlText::isXmlChar).forEach((point) -> escaped.append(escaped(point)));
		return escaped.toString();
	}

	private static String escaped(int point) {
		if (ESCAPED.containsKey(point)) {
			return ESCAPED.get(point);
		} else {
			return Character.toString(point);
		}
	}

	/** The characters XML 1.0 allows; the others (most control characters) would make the whole feed unparsable. */
	private static boolean isXmlChar(int point) {
		return point == 0x9 || 
		       point == 0xA || 
			   point == 0xD || 
			   (point >= 0x20 && point <= 0xD7FF) ||
			   (point >= 0xE000 && point <= 0xFFFD) || 
			   (point >= 0x10000 && point <= 0x10FFFF);
	}

}
