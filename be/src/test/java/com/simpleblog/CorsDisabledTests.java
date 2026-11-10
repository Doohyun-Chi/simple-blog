package com.simpleblog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/** Without {@code simpleblog.cors.allowed-origin-patterns} (the default), no origin is allowed. */
@SpringBootTest
@AutoConfigureMockMvc
class CorsDisabledTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void allowsNoOriginByDefault() throws Exception {
		this.mockMvc.perform(options("/public/api/v1/pages").header(HttpHeaders.ORIGIN, "http://localhost:5173")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

}
