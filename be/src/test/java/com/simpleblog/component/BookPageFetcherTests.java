package com.simpleblog.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import com.simpleblog.exception.BookPreviewUnavailableException;
import com.simpleblog.exception.InvalidBookException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookPageFetcherTests {

	private static final String HTML = "text/html; charset=utf-8";

	private HttpServer server;

	private BookPageFetcher fetcher;

	@BeforeEach
	void startServer() throws IOException {
		this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		this.server.start();
		// The real check refuses the loopback address the test server lives on, so it is replaced by one that only refuses /private.
		this.fetcher = new BookPageFetcher(new BookStoreUrl() {
			@Override
			public URI publicUri(String url) {
				if (url.endsWith("/private")) {
					throw new InvalidBookException("url points to the server's own network");
				}
				return URI.create(url);
			}
		});
	}

	@AfterEach
	void stopServer() {
		this.server.stop(0);
	}

	@Test
	void returnsTheHtmlOfAPage() {
		serve("/book", 200, HTML, "<title>가</title>".getBytes(StandardCharsets.UTF_8));
		assertThat(this.fetcher.page(uri("/book"))).isEqualTo("<title>가</title>");
	}

	@Test
	void decodesThePageWithTheCharsetItDeclaresAndFallsBackToUtf8() {
		serve("/euc", 200, "text/html; charset=\"EUC-KR\"", "가".getBytes(Charset.forName("EUC-KR")));
		serve("/unknown", 200, "text/html; charset=nope", "가".getBytes(StandardCharsets.UTF_8));
		assertThat(this.fetcher.page(uri("/euc"))).isEqualTo("가");
		assertThat(this.fetcher.page(uri("/unknown"))).isEqualTo("가");
	}

	@Test
	void inflatesAPageThatWasSentGzippedUnasked() throws IOException {
		serve("/gz", 200, HTML, gzip("<p>zipped</p>"));
		assertThat(this.fetcher.page(uri("/gz"))).isEqualTo("<p>zipped</p>");
	}

	@Test
	void readsNoMoreThanTheLimit() {
		serve("/big", 200, HTML, "a".repeat(BookPageFetcher.MAX_BYTES + 500).getBytes(StandardCharsets.UTF_8));
		assertThat(this.fetcher.page(uri("/big"))).hasSize(BookPageFetcher.MAX_BYTES);
	}

	@Test
	void followsARedirectAndResolvesARelativeLocation() {
		redirect("/old", "/new");
		serve("/new", 200, HTML, "moved".getBytes(StandardCharsets.UTF_8));
		assertThat(this.fetcher.page(uri("/old"))).isEqualTo("moved");
	}

	@Test
	void givesUpAfterTooManyRedirects() {
		redirect("/loop", "/loop");
		assertThatThrownBy(() -> this.fetcher.page(uri("/loop"))).isInstanceOf(BookPreviewUnavailableException.class)
			.hasMessageContaining("redirects too often");
	}

	@Test
	void refusesARedirectToAnAddressThatIsNotAllowed() {
		redirect("/old", "/private");
		assertThatThrownBy(() -> this.fetcher.page(uri("/old"))).isInstanceOf(BookPreviewUnavailableException.class)
			.hasMessageContaining("not allowed");
	}

	@Test
	void refusesARedirectWithoutALocation() {
		serve("/bare", 302, HTML, new byte[0]);
		assertThatThrownBy(() -> this.fetcher.page(uri("/bare"))).isInstanceOf(BookPreviewUnavailableException.class)
			.hasMessageContaining("Bad redirect");
	}

	@Test
	void refusesWhatIsNotAnHtmlPage() {
		serve("/missing", 404, HTML, "gone".getBytes(StandardCharsets.UTF_8));
		serve("/image", 200, "image/png", new byte[] { 1, 2, 3 });
		assertThatThrownBy(() -> this.fetcher.page(uri("/missing"))).isInstanceOf(BookPreviewUnavailableException.class)
			.hasMessageContaining("not a page");
		assertThatThrownBy(() -> this.fetcher.page(uri("/image"))).isInstanceOf(BookPreviewUnavailableException.class)
			.hasMessageContaining("not a page");
	}

	@Test
	void reportsAServerThatCannotBeReached() {
		URI unreachable = uri("/gone");
		this.server.stop(0);
		assertThatThrownBy(() -> this.fetcher.page(unreachable)).isInstanceOf(BookPreviewUnavailableException.class)
			.hasMessageContaining("could not be fetched");
	}

	private URI uri(String path) {
		return URI.create("http://127.0.0.1:" + this.server.getAddress().getPort() + path);
	}

	private void serve(String path, int status, String contentType, byte[] body) {
		this.server.createContext(path, exchange -> respond(exchange, status, contentType, body, null));
	}

	private void redirect(String path, String location) {
		this.server.createContext(path, exchange -> respond(exchange, 302, HTML, new byte[0], location));
	}

	private void respond(HttpExchange exchange, int status, String contentType, byte[] body, String location) throws IOException {
		exchange.getResponseHeaders().set("Content-Type", contentType);
		if (location != null) {
			exchange.getResponseHeaders().set("Location", location);
		}
		exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
		if (body.length > 0) {
			exchange.getResponseBody().write(body);
		}
		exchange.close();
	}

	private byte[] gzip(String text) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
			out.write(text.getBytes(StandardCharsets.UTF_8));
		}
		return bytes.toByteArray();
	}

}
