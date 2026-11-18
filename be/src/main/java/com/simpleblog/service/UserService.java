package com.simpleblog.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import com.simpleblog.entity.BlogUser;
import com.simpleblog.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * The users of TB_USER. Not transactional on purpose: each repository call is its own transaction, so a lost race to
 * register the same email (a unique violation) leaves nothing half done and the winner's row can simply be read.
 */
@Service
public class UserService {

	static final String GOOGLE = "google";

	private final UserRepository userRepository;

	private final Clock clock;

	public UserService(UserRepository userRepository, Clock clock) {
		this.userRepository = userRepository;
		this.clock = clock;
	}

	/** The user with this email, registered with a new id when there is none yet. */
	public BlogUser registration(String email, String provider) {
		String normalized = normalized(email);
		return this.userRepository.findByEmail(normalized).orElseGet(() -> register(normalized, provider));
	}

	/** The id to record as the author or owner of what a signed-in Google user creates. */
	public long idOf(String email) {
		return registration(email, GOOGLE).id();
	}

	private BlogUser register(String email, String provider) {
		try {
			return this.userRepository.save(new BlogUser(email, provider, LocalDateTime.now(this.clock.withZone(ZoneOffset.UTC))));
		}
		catch (DataIntegrityViolationException ex) {
			return this.userRepository.findByEmail(email).orElseThrow(() -> ex);
		}
	}

	private String normalized(String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("A user needs an email");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
