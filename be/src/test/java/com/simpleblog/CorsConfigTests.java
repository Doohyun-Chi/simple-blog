package com.simpleblog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = "simpleblog.cors.allowed-origin-patterns=http://localhost:[*]")
@AutoConfigureMockMvc
class CorsConfigTests {

	private static final String PAGES = "/public/api/v1/pages";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void allowsPreflightFromConfiguredOrigin() throws Exception {
		this.mockMvc.perform(preflight(PAGES, "http://localhost:5173"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
	}

	@Test
	void allowsPreflightFromAnyLocalhostPort() throws Exception {
		this.mockMvc.perform(preflight(PAGES, "http://localhost:3000"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
	}

	@Test
	void rejectsPreflightFromOtherHostOnSamePort() throws Exception {
		this.mockMvc.perform(preflight(PAGES, "http://localhost.evil.example:5173"))
			.andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void rejectsPreflightFromOtherOrigin() throws Exception {
		this.mockMvc.perform(preflight(PAGES, "http://evil.example"))
			.andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void doesNotApplyOutsideThePublicApi() throws Exception {
		this.mockMvc.perform(preflight("/actuator/health", "http://localhost:5173"))
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	private static MockHttpServletRequestBuilder preflight(String path, String origin) {
		return options(path).header(HttpHeaders.ORIGIN, origin)
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
	}

}
