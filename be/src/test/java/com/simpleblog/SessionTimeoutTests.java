package com.simpleblog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/** A signed-in session lasts an hour from its last request. */
@SpringBootTest
class SessionTimeoutTests {

	@Value("${server.servlet.session.timeout}")
	private Duration timeout;

	@Test
	void aSessionEndsAfterAnHourWithoutARequest() {
		assertThat(this.timeout).isEqualTo(Duration.ofHours(1));
	}

}
