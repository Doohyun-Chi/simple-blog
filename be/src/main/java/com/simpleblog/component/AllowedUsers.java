package com.simpleblog.component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The emails allowed to sign in, from {@code simpleblog.auth.allowed-emails} (comma separated, case-insensitive). With
 * none listed nobody is allowed.
 */
@Component
public class AllowedUsers {

	private final Set<String> emails;

	public AllowedUsers(@Value("${simpleblog.auth.allowed-emails:}") String[] emails) {
		this.emails = Arrays.stream(emails).map(AllowedUsers::normalized).filter((email) -> !email.isEmpty()).collect(Collectors.toSet());
	}

	/** Whether this email is listed. A missing email is never allowed. */
	public boolean permits(String email) {
		return email != null && this.emails.contains(normalized(email));
	}

	private static String normalized(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
