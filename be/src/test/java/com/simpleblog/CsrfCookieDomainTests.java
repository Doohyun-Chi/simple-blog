package com.simpleblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "simpleblog.csrf.cookie-domain=chidoo.me")
@AutoConfigureMockMvc
class CsrfCookieDomainTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void publishesTheTokenForTheParentDomainSoAFrontendOnAnotherHostCanRead() throws Exception {
		String cookie = this.mockMvc.perform(get("/public/api/v1/posts")).andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
		assertThat(cookie).startsWith("XSRF-TOKEN=").contains("Domain=chidoo.me");
	}

}
