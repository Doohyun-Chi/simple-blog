package com.simpleblog.component;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PostExcerptTests {

	private static final String ORIGIN = "https://blog.chidoo.me";

	private final PostExcerpt excerpt = new PostExcerpt();

	@Test
	void keepsATextThatFitsWhole() {
		assertThat(this.excerpt.descriptionOf("짧은 글입니다.")).isEqualTo("짧은 글입니다.");
	}

	@Test
	void stripsMarkdownHtmlAndBlockComments() {
		String content = "<!-- wp:paragraph --><p>Hello &amp; <b>world</b></p><!-- /wp:paragraph -->\n\n# Title\n\n**bold** [a link](http://x.example) ![pic](/a.png)";
		assertThat(this.excerpt.descriptionOf(content)).isEqualTo("Hello & world Title bold a link");
	}

	@Test
	void dropsFencedCodeBlocksSuchAsMermaidDiagrams() {
		String content = "Before\n\n```mermaid\ngraph TD\n  A-->B\n```\n\nMiddle\n\n~~~~java\nint x;\n~~~~\n\nAfter";
		assertThat(this.excerpt.descriptionOf(content)).isEqualTo("Before Middle After");
	}

	@Test
	void dropsAnUnclosedFenceToTheEndAndKeepsInlineCode() {
		assertThat(this.excerpt.descriptionOf("Use `x` here\n\n```\nnever closed")).isEqualTo("Use x here");
	}

	@Test
	void cutsBeforeAWordAndAddsAnEllipsis() {
		String content = "word ".repeat(60);
		String description = this.excerpt.descriptionOf(content);
		assertThat(description).endsWith("word…").hasSizeLessThanOrEqualTo(201);
		assertThat(description.replace("…", "").split(" ")).containsOnly("word");
	}

	@Test
	void doesNotBreakAWordThatStraddlesTheLimit() {
		String content = "a".repeat(195) + " bbbbbbbbbb cc";
		assertThat(this.excerpt.descriptionOf(content)).isEqualTo("a".repeat(195) + "…");
	}

	@Test
	void cutsAtTheLimitWhenTheFirstWordIsLongerThanIt() {
		assertThat(this.excerpt.descriptionOf("x".repeat(300))).isEqualTo("x".repeat(200) + "…");
	}

	@Test
	void findsTheFirstImageOfEitherKind() {
		assertThat(this.excerpt.firstImageOf("text ![a](https://cdn.example/1.jpg) <img src=\"/2.png\">", ORIGIN))
			.contains("https://cdn.example/1.jpg");
		assertThat(this.excerpt.firstImageOf("<p><img class=\"x\" src='https://cdn.example/2.png'></p> ![a](/1.jpg)", ORIGIN))
			.contains("https://cdn.example/2.png");
	}

	@Test
	void resolvesARelativeSourceAgainstTheOrigin() {
		assertThat(this.excerpt.firstImageOf("![a](/public/api/v1/media/thumb)", ORIGIN)).contains(ORIGIN + "/public/api/v1/media/thumb");
	}

	@Test
	void skipsAnImageThatIsNotAWebAddress() {
		assertThat(this.excerpt.firstImageOf("![a](data:image/png;base64,AAAA) ![b](https://cdn.example/b.png)", ORIGIN))
			.contains("https://cdn.example/b.png");
		assertThat(this.excerpt.firstImageOf("no image here", ORIGIN)).isEmpty();
	}

}
