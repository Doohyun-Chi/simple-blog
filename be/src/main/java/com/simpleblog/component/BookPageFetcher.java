package com.simpleblog.component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.simpleblog.exception.InvalidBookException;
import com.simpleblog.exception.BookPreviewUnavailableException;
import org.springframework.stereotype.Component;

/**
 * Fetches the page of a book at its store. Redirects are followed by hand, at most {@link #MAX_REDIRECTS}, and each target is
 * checked like the first URL; a page is read up to {@link #MAX_BYTES} and only when it is HTML.
 */
@Component
public class BookPageFetcher {

	static final int MAX_REDIRECTS = 3;

	static final int MAX_BYTES = 1_000_000;

	private static final Pattern CHARSET = Pattern.compile("charset=\"?([\\w-]+)", Pattern.CASE_INSENSITIVE);

	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	private final BookStoreUrl storeUrl;

	private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(TIMEOUT).build();

	public BookPageFetcher(BookStoreUrl storeUrl) {
		this.storeUrl = storeUrl;
	}

	/** The HTML of the page; {@link BookPreviewUnavailableException} when it cannot be fetched. */
	public String page(URI uri) {
		URI target = uri;
		for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
			HttpResponse<InputStream> response = response(target);
			if (response.statusCode() / 100 == 3) {
				target = redirectTarget(target, response);
				continue;
			}
			return html(response);
		}
		throw new BookPreviewUnavailableException("The page redirects too often", null);
	}

	private HttpResponse<InputStream> response(URI uri) {
		HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).header("Accept", "text/html").header("Accept-Encoding", "identity")
			.header("User-Agent", "Mozilla/5.0 (compatible; simple-blog)").GET().build();
		try {
			return this.client.send(request, HttpResponse.BodyHandlers.ofInputStream());
		}
		catch (IOException ex) {
			throw new BookPreviewUnavailableException("The page could not be fetched", ex);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new BookPreviewUnavailableException("The fetch was interrupted", ex);
		}
	}

	private URI redirectTarget(URI from, HttpResponse<InputStream> response) {
		String location = response.headers().firstValue("Location").orElseThrow(() -> new BookPreviewUnavailableException("Bad redirect", null));
		try {
			return this.storeUrl.publicUri(from.resolve(location).toString());
		}
		catch (InvalidBookException | IllegalArgumentException ex) {
			throw new BookPreviewUnavailableException("The page redirects to an address that is not allowed", ex);
		}
	}

	private String html(HttpResponse<InputStream> response) {
		String type = response.headers().firstValue("Content-Type").orElse("");
		if (response.statusCode() != 200 || !type.toLowerCase(Locale.ROOT).contains("html")) {
			throw new BookPreviewUnavailableException("The address is not a page", null);
		}
		try (InputStream body = response.body()) {
			return new String(plain(body.readNBytes(MAX_BYTES)), charsetOf(type));
		}
		catch (IOException ex) {
			throw new BookPreviewUnavailableException("The page could not be read", ex);
		}
	}

	/** The bytes as they are, or inflated when a server sent them gzipped though the request did not ask for it, as Amazon does. */
	private byte[] plain(byte[] bytes) throws IOException {
		boolean gzipped = bytes.length > 2 && (bytes[0] & 0xff) == 0x1f && (bytes[1] & 0xff) == 0x8b;
		if (!gzipped) {
			return bytes;
		}
		try (GZIPInputStream inflated = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
			return inflated.readNBytes(MAX_BYTES);
		}
	}

	private Charset charsetOf(String contentType) {
		Matcher matcher = CHARSET.matcher(contentType);
		try {
			return matcher.find() ? Charset.forName(matcher.group(1)) : StandardCharsets.UTF_8;
		}
		catch (IllegalArgumentException ex) {
			return StandardCharsets.UTF_8;
		}
	}

}
