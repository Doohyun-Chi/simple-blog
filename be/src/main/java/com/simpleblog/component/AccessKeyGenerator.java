package com.simpleblog.component;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

/** Makes the MCP access keys: 32 random bytes from a secure source, as URL safe text, so a key cannot be guessed. */
@Component
public class AccessKeyGenerator {

	private static final int BYTES = 32;

	private final SecureRandom random = new SecureRandom();

	public String key() {
		byte[] bytes = new byte[BYTES];
		this.random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

}
