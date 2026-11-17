package com.simpleblog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** With the guard on but no write token configured, every write is refused with 403, whatever the request carries. */
@SpringBootTest(properties = "simpleblog.write.token=")
@AutoConfigureMockMvc
class PostWriteWithoutTokenTests {

	private static final String BODY = "{\"title\": \"t\", \"slug\": \"s\", \"content\": \"c\"}";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void refusesToCreateWithForbidden() throws Exception {
		this.mockMvc.perform(post("/public/api/v1/post").header("Authorization", "Bearer ")
			.contentType(MediaType.APPLICATION_JSON)
			.content(BODY)).andExpect(status().isForbidden());
	}

	@Test
	void refusesToUpdateWithForbidden() throws Exception {
		this.mockMvc.perform(put("/public/api/v1/post/s").contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isForbidden());
	}

}
