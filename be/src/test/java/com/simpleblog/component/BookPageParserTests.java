package com.simpleblog.component;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import com.simpleblog.dto.BookPreview;
import org.junit.jupiter.api.Test;

class BookPageParserTests {

	private static final URI PAGE = URI.create("https://store.example/books/1");

	private final BookPageParser parser = new BookPageParser();

	@Test
	void readsTheOpenGraphMetasAndDecodesTheirEntities() {
		String html = """
				<meta property='og:title' content='Tom &amp; Jerry &#39;s &#xAC00;'>
				<META NAME="Author" CONTENT="A. Writer">
				<meta property="og:description" content="Line one
				  line two">
				<meta property="og:image" content="https://img.example/a.jpg">""";
		assertThat(this.parser.preview(html, PAGE)).isEqualTo(new BookPreview("Tom & Jerry 's 가", "A. Writer", "Line one line two",
				"https://img.example/a.jpg"));
	}

	@Test
	void fallsBackToTheTitleTagAndGivesNullForWhatThePageLacks() {
		BookPreview preview = this.parser.preview("<title> Plain  title </title><meta name=\"author\" content=\"https://x.example/a\">", PAGE);
		assertThat(preview).isEqualTo(new BookPreview("Plain title", null, null, null));
	}

	@Test
	void resolvesARelativeImageAndDropsOneThatIsNotHttp() {
		assertThat(this.parser.preview("<meta property=\"og:image\" content=\"../i/a.png\">", PAGE).thumbnailUrl())
			.isEqualTo("https://store.example/i/a.png");
		assertThat(this.parser.preview("<meta property=\"og:image\" content=\"data:image/png;base64,AA\">", PAGE).thumbnailUrl()).isNull();
	}

	@Test
	void cutsValuesToTheLimitsOfTheirFieldsAndSurvivesBadEntities() {
		String html = "<meta property=\"og:title\" content=\"" + "가".repeat(400) + "&#99999999999;\">";
		assertThat(this.parser.preview(html, PAGE).title()).hasSize(300);
	}

	@Test
	void keepsOnlyTheTitleOfAYes24Page() {
		URI yes24 = URI.create("https://www.yes24.com/Product/Goods/1");
		String html = "<meta property=\"og:title\" content=\"감정 | 데이비드 | 북파머스 - 예스24\">";
		assertThat(this.parser.preview(html, yes24).title()).isEqualTo("감정");
		assertThat(this.parser.preview(html, PAGE).title()).isEqualTo("감정 | 데이비드 | 북파머스 - 예스24");
	}

	@Test
	void readsAnAmazonPageFromItsElementsAndDropsTheShippingDescription() {
		String html = """
				<meta name="description" content="Title on Amazon.com. *FREE* shipping on qualifying offers.">
				<span id="productTitle" class="x">
				   Architecture for Flow &amp; More   </span>
				<div id="bylineInfo" class="a-section">by <span class="author notFaded" data-width="">
				<a class="a-link-normal" href="/e/1">Susanne Kaiser</a></span>
				<span class="author notFaded"><a href="/e/2">Second Author</a></span></div>
				<img alt="" id="landingImage" data-a-dynamic-image="{&quot;https://m.media-amazon.com/images/I/a._SY342_.jpg&quot;:[342,261],
				&quot;https://m.media-amazon.com/images/I/a._SY522_.jpg&quot;:[522,399]}" />""";
		assertThat(this.parser.preview(html, URI.create("https://www.amazon.com/dp/1"))).isEqualTo(new BookPreview("Architecture for Flow & More",
				"Susanne Kaiser, Second Author", null, "https://m.media-amazon.com/images/I/a._SY522_.jpg"));
	}

}
