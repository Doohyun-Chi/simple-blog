package com.simpleblog.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;

import com.simpleblog.entity.BlogUser;
import com.simpleblog.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * The users of TB_USER. Not transactional on purpose: each repository call is its own transaction, so a lost race to
 * register the same email (a unique violation) leaves nothing half done and the winner's row can simply be read.
 */
@Service
public class UserService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

	/** The viewer id of a visitor who is not signed in. Ids start at 1, so it never matches an author. */
	public static final long ANONYMOUS = 0;

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

	/** The id of whoever is reading: the signed-in user's, or {@link #ANONYMOUS} (no user has it) for an anonymous visitor. */
	public long viewerId(OidcUser user) {
		return user == null ? ANONYMOUS : idOf(user.getEmail());
	}

	/**
	 * The id of the user whose session makes the current request, or {@link #ANONYMOUS} when there is none. It only reads:
	 * a user is registered when signing in, so one without a row cannot have written anything.
	 */
	public long currentViewerId() {
		Optional<String> email = currentEmail();
		long id = email.flatMap((value) -> this.userRepository.findByEmail(normalized(value))).map(BlogUser::id).orElse(ANONYMOUS);
		LOGGER.info("Current viewer: session email={}, id={}, authentication={}", email.orElse(null), id, authenticationType());
		return id;
	}

	private String authenticationType() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return null;
		}
		return authentication.getClass().getSimpleName() + "/" + authentication.getPrincipal().getClass().getSimpleName();
	}

	/** The id to record as the author of what the signed-in user of the current request creates, registering the user on first use. */
	public long currentWriterId() {
		return idOf(currentEmail().orElseThrow(() -> new IllegalStateException("There is no signed-in user for this request")));
	}

	private Optional<String> currentEmail() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof OidcUser user) {
			return Optional.ofNullable(user.getEmail()).filter((email) -> !email.isBlank());
		}
		return Optional.empty();
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
