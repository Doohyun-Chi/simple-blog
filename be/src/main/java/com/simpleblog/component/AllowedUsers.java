package com.simpleblog.component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The emails allowed to sign in, from {@code simpleblog.auth.allowed-emails} (comma separated, case-insensitive). When
 * emails are listed only they are allowed; with none listed every signed-in Google user is.
 */
@Component
public class AllowedUsers {

	private final Set<String> emails;

	public AllowedUsers(@Value("${simpleblog.auth.allowed-emails:}") String[] emails) {
		this.emails = Arrays.stream(emails).map(AllowedUsers::normalized).filter((email) -> !email.isEmpty()).collect(Collectors.toSet());
	}

	/** Whether this email may sign in: listed, or anyone when nothing is listed. A missing or blank email never is. */
	public boolean permits(String email) {
		if (email == null || email.isBlank()) {
			return false;
		}
		return this.emails.isEmpty() || this.emails.contains(normalized(email));
	}

	private static String normalized(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
