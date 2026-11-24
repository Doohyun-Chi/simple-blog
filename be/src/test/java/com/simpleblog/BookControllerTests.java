package com.simpleblog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.simpleblog.component.BookPageFetcher;
import com.simpleblog.exception.BookPreviewUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * The bookshelf API of be/bookapispec.yaml against H2. The clock is fixed: 2026-11-17 01:30 UTC is 2026-11-17 in Asia/Seoul.
 * The books 1001 (2025) and 1002 (2026, being read) belong to user 1, the owner; 1003 (2026) to user 2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql(statements = """
		insert into tb_user (ID, EMAIL, PROVIDER, REGISTERED_DATE) values
		(1, 'owner@example.com', 'google', now()),
		(2, 'other@example.com', 'google', now());
		insert into tb_book (ID, TITLE, AUTHOR, BOOKSTORE_URL, REGISTERED_DATE, READING_START_DATE, READER_ID) values
		(1001, 'Old book', 'A', 'https://store.example/1', '2025-03-01', null, 1),
		(1002, 'Reading', 'B', 'https://store.example/2', '2026-05-01', '2026-05-02', 1),
		(1003, 'Theirs', 'C', 'https://store.example/3', '2026-06-01', null, 2);
		""")
class BookControllerTests {

	private static final String BOOK = "/public/api/v1/book";

	@TestConfiguration
	static class FixedClock {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(Instant.parse("2026-11-17T01:30:00Z"), ZoneId.of("Asia/Seoul"));
		}

	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BookPageFetcher pageFetcher;

	private MockHttpServletRequestBuilder asOwner(MockHttpServletRequestBuilder request, String body) {
		return request.with(TestUsers.signedInWithId("owner@example.com", 1)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private ResultActions putAsOwner(String path, String body) throws Exception {
		return this.mockMvc.perform(asOwner(put(BOOK + path), body));
	}

	@Test
	void listsTheBooksNewestFirstWithTheYearsToAnonymousVisitors() throws Exception {
		this.mockMvc.perform(get("/public/api/v1/books"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.years[0]").value(2026))
			.andExpect(jsonPath("$.years[1]").value(2025))
			.andExpect(jsonPath("$.books[0].id").value(1003))
			.andExpect(jsonPath("$.books[2].id").value(1001))
			.andExpect(jsonPath("$.books[1].readingStartDate").value("2026-05-02"))
			.andExpect(jsonPath("$.books[1].readerId").doesNotExist());
	}

	@Test
	void keepsTheBooksOfTheYearButAllTheYears() throws Exception {
		this.mockMvc.perform(get("/public/api/v1/books").param("year", "2025"))
			.andExpect(jsonPath("$.books.length()").value(1))
			.andExpect(jsonPath("$.books[0].id").value(1001))
			.andExpect(jsonPath("$.years.length()").value(2));
	}

	@Test
	void refusesAYearThatIsNotFourDigits() throws Exception {
		this.mockMvc.perform(get("/public/api/v1/books").param("year", "26")).andExpect(status().isBadRequest());
		this.mockMvc.perform(get("/public/api/v1/books").param("year", "abc")).andExpect(status().isBadRequest());
	}

	@Test
	void readsABookAndAnswers404ForAnUnknownOne() throws Exception {
		this.mockMvc.perform(get(BOOK + "/1001")).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Old book"));
		this.mockMvc.perform(get(BOOK + "/9999")).andExpect(status().isNotFound());
	}

	@Test
	void registersAnUnreadBookDatedTodayForTheSignedInUser() throws Exception {
		String body = """
				{"title": " 클린 코드 ", "author": "Robert", "bookstoreUrl": "https://www.yes24.com/Product/Goods/1",
				 "thumbnailUrl": "", "description": "desc"}""";
		this.mockMvc.perform(asOwner(post(BOOK), body))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.title").value("클린 코드"))
			.andExpect(jsonPath("$.registeredDate").value("2026-11-17"))
			.andExpect(jsonPath("$.thumbnailUrl").isEmpty())
			.andExpect(jsonPath("$.readingStartDate").isEmpty());
	}

	@Test
	void refusesABookWithoutATitleOrWithABadUrl() throws Exception {
		this.mockMvc.perform(asOwner(post(BOOK), "{\"title\": \" \", \"bookstoreUrl\": \"https://a.example/x\"}")).andExpect(status().isBadRequest());
		this.mockMvc.perform(asOwner(post(BOOK), "{\"title\": \"T\", \"bookstoreUrl\": \"ftp://a.example/x\"}")).andExpect(status().isBadRequest());
		this.mockMvc.perform(asOwner(post(BOOK), "{\"title\": \"T\"}")).andExpect(status().isBadRequest());
		this.mockMvc
			.perform(asOwner(post(BOOK), "{\"title\": \"T\", \"bookstoreUrl\": \"https://a.example/x\", \"thumbnailUrl\": \"javascript:x\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void requiresASignedInUserAndTheCsrfToken() throws Exception {
		String body = "{\"title\": \"T\", \"bookstoreUrl\": \"https://a.example/x\"}";
		this.mockMvc.perform(post(BOOK).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
		this.mockMvc.perform(post(BOOK).with(TestUsers.signedInWithId("owner@example.com", 1)).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isForbidden());
	}

	@Test
	void editsTheDetailsAndKeepsTheReadingAndTheDate() throws Exception {
		String body = "{\"title\": \"New\", \"author\": \"Z\", \"bookstoreUrl\": \"https://a.example/n\"}";
		putAsOwner("/1002", body).andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("New"))
			.andExpect(jsonPath("$.readingStartDate").value("2026-05-02"))
			.andExpect(jsonPath("$.registeredDate").value("2026-05-01"));
	}

	@Test
	void keepsABookFromAnyoneButItsReader() throws Exception {
		String body = "{\"title\": \"New\", \"bookstoreUrl\": \"https://a.example/n\"}";
		putAsOwner("/1003", body).andExpect(status().isForbidden());
		putAsOwner("/1003/insight", "{\"insight\": \"x\"}").andExpect(status().isForbidden());
		this.mockMvc.perform(asOwner(delete(BOOK + "/1003"), "")).andExpect(status().isForbidden());
		putAsOwner("/9999", body).andExpect(status().isNotFound());
	}

	@Test
	void deletesABook() throws Exception {
		this.mockMvc.perform(asOwner(delete(BOOK + "/1001"), "")).andExpect(status().isNoContent());
		this.mockMvc.perform(get(BOOK + "/1001")).andExpect(status().isNotFound());
	}

	@Test
	void startsAndFinishesABookAndClearsTheCompletion() throws Exception {
		putAsOwner("/1001/reading", "{\"readingStartDate\": \"2026-11-01\"}").andExpect(jsonPath("$.readingStartDate").value("2026-11-01"))
			.andExpect(jsonPath("$.readingCompleteDate").isEmpty());
		putAsOwner("/1001/reading", "{\"readingStartDate\": \"2026-11-01\", \"readingCompleteDate\": \"2026-11-17\"}")
			.andExpect(jsonPath("$.readingCompleteDate").value("2026-11-17"));
		putAsOwner("/1001/reading", "{\"readingStartDate\": \"2026-11-01\"}").andExpect(jsonPath("$.readingCompleteDate").isEmpty());
	}

	@Test
	void refusesReadingDatesThatDoNotAddUp() throws Exception {
		putAsOwner("/1001/reading", "{\"readingCompleteDate\": \"2026-11-17\"}").andExpect(status().isBadRequest());
		putAsOwner("/1001/reading", "{\"readingStartDate\": \"2026-11-10\", \"readingCompleteDate\": \"2026-11-09\"}")
			.andExpect(status().isBadRequest());
		putAsOwner("/1001/reading", "{\"readingStartDate\": \"2026-11-18\"}").andExpect(status().isBadRequest());
		putAsOwner("/1001/reading", "{\"readingStartDate\": \"nope\"}").andExpect(status().isBadRequest());
	}

	@Test
	void savesAndClearsTheInsightAndKeepsItWhole() throws Exception {
		String long300 = "가".repeat(300);
		putAsOwner("/1002/insight", "{\"insight\": \"" + long300 + "\"}").andExpect(jsonPath("$.insight").value(long300));
		putAsOwner("/1002/insight", "{\"insight\": \"\"}").andExpect(jsonPath("$.insight").isEmpty());
		putAsOwner("/1002/insight", "{\"insight\": \"" + "a".repeat(5001) + "\"}").andExpect(status().isBadRequest());
		putAsOwner("/1002/insight", "{}").andExpect(status().isBadRequest());
	}

	@Test
	void linksAndUnlinksAReviewSlug() throws Exception {
		putAsOwner("/1002/review", "{\"trackbackSlug\": \"review-about-clean-code\"}")
			.andExpect(jsonPath("$.trackbackSlug").value("review-about-clean-code"));
		putAsOwner("/1002/review", "{\"trackbackSlug\": null}").andExpect(jsonPath("$.trackbackSlug").isEmpty());
		putAsOwner("/1002/review", "{\"trackbackSlug\": \"클린\"}").andExpect(status().isBadRequest());
		putAsOwner("/1002/review", "{\"trackbackSlug\": \"" + "a".repeat(201) + "\"}").andExpect(status().isBadRequest());
	}

	@Test
	void previewsABookFromItsStorePage() throws Exception {
		when(this.pageFetcher.page(any())).thenReturn("""
				<html><head><meta property="og:title" content="클린 코드"><meta name="author" content="Robert">
				<meta property="og:image" content="/img/1.jpg"></head></html>""");
		this.mockMvc.perform(get("/public/api/v1/book-preview").param("url", "https://93.184.216.34/p/1").with(TestUsers.signedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("클린 코드"))
			.andExpect(jsonPath("$.author").value("Robert"))
			.andExpect(jsonPath("$.description").isEmpty())
			.andExpect(jsonPath("$.thumbnailUrl").value("https://93.184.216.34/img/1.jpg"));
	}

	@Test
	void refusesToPreviewForAnonymousVisitorsAndForTheServersOwnNetwork() throws Exception {
		this.mockMvc.perform(get("/public/api/v1/book-preview").param("url", "https://93.184.216.34/p/1")).andExpect(status().isUnauthorized());
		for (String url : new String[] { "http://127.0.0.1/x", "http://10.0.0.5/x", "http://192.168.1.1/x", "http://169.254.169.254/x",
				"http://[::1]/x", "ftp://93.184.216.34/x", "not a url" }) {
			this.mockMvc.perform(get("/public/api/v1/book-preview").param("url", url).with(TestUsers.signedIn()))
				.andExpect(status().isBadRequest());
		}
		this.mockMvc.perform(get("/public/api/v1/book-preview").with(TestUsers.signedIn())).andExpect(status().isBadRequest());
	}

	@Test
	void answers502WhenTheStorePageCannotBeFetched() throws Exception {
		when(this.pageFetcher.page(any())).thenThrow(new BookPreviewUnavailableException("down", null));
		this.mockMvc.perform(get("/public/api/v1/book-preview").param("url", "https://93.184.216.34/p/1").with(TestUsers.signedIn()))
			.andExpect(status().isBadGateway());
	}

}
