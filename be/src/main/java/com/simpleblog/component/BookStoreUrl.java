package com.simpleblog.component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

import com.simpleblog.exception.InvalidBookException;
import org.springframework.stereotype.Component;

/**
 * The URL of a book's store page the server is asked to fetch. Only http and https URLs of public hosts pass: a host that is,
 * or resolves to, an address of the server's own network (loopback, private, link-local and the like) is refused, so the
 * preview cannot be used to reach what only the server can.
 */
@Component
public class BookStoreUrl {

	public static final int MAX_LENGTH = 2000;

	/** The URI of the text, or {@link InvalidBookException} when it is not an http or https URL of a public host. */
	public URI publicUri(String url) {
		URI uri = uriOf(url);
		String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
		if (!scheme.equals("http") && !scheme.equals("https") || uri.getHost() == null) {
			throw new InvalidBookException("url must be an http or https URL");
		}
		requirePublicHost(uri.getHost());
		return uri;
	}

	/** Whether the text is an http or https URL; the host is not looked at. */
	public static boolean isWebUrl(String url) {
		try {
			URI uri = new URI(url);
			return uri.getHost() != null && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
		}
		catch (URISyntaxException ex) {
			return false;
		}
	}

	private URI uriOf(String url) {
		if (url == null || url.isBlank() || url.length() > MAX_LENGTH) {
			throw new InvalidBookException("url is required and limited to " + MAX_LENGTH + " characters");
		}
		try {
			return new URI(url.trim());
		}
		catch (URISyntaxException ex) {
			throw new InvalidBookException("url is not a valid URL");
		}
	}

	private void requirePublicHost(String host) {
		try {
			for (InetAddress address : InetAddress.getAllByName(host)) {
				if (!isPublic(address)) {
					throw new InvalidBookException("url points to the server's own network");
				}
			}
		}
		catch (UnknownHostException ex) {
			throw new InvalidBookException("url has an unknown host");
		}
	}

	private boolean isPublic(InetAddress address) {
		return !(address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress()
				|| address.isMulticastAddress() || isUniqueLocalOrShared(address.getAddress()));
	}

	/** The IPv6 unique local range fc00::/7 and the IPv4 shared range 100.64.0.0/10, which the JDK does not call site local. */
	private boolean isUniqueLocalOrShared(byte[] bytes) {
		if (bytes.length == 16) {
			return (bytes[0] & 0xfe) == 0xfc;
		}
		return (bytes[0] & 0xff) == 100 && (bytes[1] & 0xc0) == 0x40;
	}

}
