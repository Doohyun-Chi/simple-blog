package com.simpleblog;

import static org.assertj.core.api.Assertions.assertThat;

import com.simpleblog.component.SlugEncoder;
import org.junit.jupiter.api.Test;

class SlugEncoderTests {

	private final SlugEncoder slugEncoder = new SlugEncoder();

	@Test
	void keepsAsciiAsIs() {
		assertThat(this.slugEncoder.encode("kpi-power-in-big-company")).isEqualTo("kpi-power-in-big-company");
	}

	@Test
	void percentEncodesNonAsciiBytesInLowercaseHex() {
		assertThat(this.slugEncoder.encode("큰기업-kpi")).isEqualTo("%ed%81%b0%ea%b8%b0%ec%97%85-kpi");
	}

}
