package com.simpleblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.simpleblog.component.SharePage;
import com.simpleblog.dto.ShareTags;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "simpleblog.auth.frontend-url=https://www.chidoo.me/")
@AutoConfigureMockMvc
@Transactional
@Sql(statements = """
		insert into tb_user (ID, EMAIL, PROVIDER, REGISTERED_DATE) values (1, 'owner@example.com', 'google', now());
		insert into tb_post (ID, TITLE, CONTENT, SLUG, TYPE, STATUS, AUTHOR_ID, CREATED_DATETIME, MODIFIED_DATETIME) values
		(1, 'A <b>"quoted"</b> title', 'Intro ![x](https://cdn.example/a.jpg) text', 'kpi', 'post', 'publish', 1, '2026-11-05 00:45:11', '2026-11-06 01:00:00'),
		(2, 'Draft', 'draft body', 'a-draft', 'post', 'draft', 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
		(3, 'Private', 'private body', 'a-private', 'post', 'private', 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
		(4, 'About', 'no image', 'about', 'page', 'publish', 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
		(5, '큰기업', '![m](/public/api/v1/media/thumb) 본문', '%ed%81%b0%ea%b8%b0%ec%97%85-kpi', 'post', 'publish', 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
		(6, 'Scan', 'scan', 'a-scan', 'attachment', 'publish', 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00');
		""")
class ShareControllerTests {

	@Autowired
	private MockMvc mockMvc;

	private String pageOf(String slug) throws Exception {
		return this.mockMvc.perform(get("/public/share/{slug}", slug))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	@Test
	void servesTheTagsAndALinkToThePostViewWithoutASession() throws Exception {
		String page = pageOf("kpi");
		assertThat(page).contains("<meta property=\"og:title\" content=\"A &lt;b&gt;&quot;quoted&quot;&lt;/b&gt; title\" />")
			.contains("<meta property=\"og:description\" content=\"Intro text\" />")
			.contains("<meta property=\"og:image\" content=\"https://cdn.example/a.jpg\" />")
			.contains("<meta name=\"twitter:card\" content=\"summary_large_image\" />")
			.contains("<a href=\"https://www.chidoo.me/post/kpi\">A &lt;b&gt;&quot;quoted&quot;&lt;/b&gt; title</a>")
			.contains("<script>location.replace(document.querySelector('a').href)</script>")
			.doesNotContain("og:url");
	}

	@Test
	void fallsBackToTheSiteImageAndLinksAPageToItsPageUrl() throws Exception {
		String page = pageOf("about");
		assertThat(page).contains("content=\"https://www.chidoo.me/realizing-future.jpg\"")
			.contains("<a href=\"https://www.chidoo.me/page/about\">About</a>");
	}

	@Test
	void findsAStoredPercentEncodedSlugByItsReaderFormAndCompletesARelativeImage() throws Exception {
		String page = pageOf("큰기업-kpi");
		assertThat(page).contains("content=\"http://localhost/public/api/v1/media/thumb\"")
			.contains("<a href=\"https://www.chidoo.me/post/%ED%81%B0%EA%B8%B0%EC%97%85-kpi\">큰기업</a>");
	}

	@Test
	void fillsEachHolderOnceEvenWhenAValueLooksLikeAHolder() {
		String page = new SharePage().render(new ShareTags("{{url}} $1", "d", "https://x/y.jpg", "https://x/z"));
		assertThat(page).contains("<title>{{url}} $1</title>").contains("<a href=\"https://x/z\">{{url}} $1</a>").doesNotContain("{{title}}");
	}

	@Test
	void hasNoSharePageForADraftAPrivatePostAnAttachmentOrAnUnknownSlug() throws Exception {
		for (String slug : new String[] { "a-draft", "a-private", "a-scan", "nothing-here" }) {
			this.mockMvc.perform(get("/public/share/" + slug)).andExpect(status().isNotFound());
		}
	}

}
