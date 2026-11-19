package com.simpleblog.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;

import com.simpleblog.component.BlogOidcUser;
import com.simpleblog.entity.BlogUser;
import com.simpleblog.repository.UserRepository;
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

	/** The id of a signed-in user: the one in their session when sign-in put it there, otherwise looked up by email. */
	public long idOf(OidcUser user) {
		return user instanceof BlogOidcUser signedIn ? signedIn.userId() : idOf(user.getEmail());
	}

	/** The id of whoever is reading: the signed-in user's, or {@link #ANONYMOUS} (no user has it) for an anonymous visitor. */
	public long viewerId(OidcUser user) {
		return user == null ? ANONYMOUS : idOf(user);
	}

	/**
	 * The id of the user whose session makes the current request, or {@link #ANONYMOUS} when there is none. It takes the
	 * id from the session; only a session that has none (made before ids were kept in it) costs a read of TB_USER by email.
	 */
	public long currentViewerId() {
		return currentUser().map(this::knownIdOf).orElse(ANONYMOUS);
	}

	/** The id of this user without registering anyone: a user with no row cannot have written anything. */
	private long knownIdOf(OidcUser user) {
		if (user instanceof BlogOidcUser signedIn) {
			return signedIn.userId();
		}
		if (user.getEmail() == null || user.getEmail().isBlank()) {
			return ANONYMOUS;
		}
		return this.userRepository.findByEmail(normalized(user.getEmail())).map(BlogUser::id).orElse(ANONYMOUS);
	}

	/** The id to record as the author of what the signed-in user of the current request creates, registering the user on first use. */
	public long currentWriterId() {
		return idOf(currentUser().orElseThrow(() -> new IllegalStateException("There is no signed-in user for this request")));
	}

	private Optional<OidcUser> currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.getPrincipal() instanceof OidcUser user ? Optional.of(user) : Optional.empty();
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
