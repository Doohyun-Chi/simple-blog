package com.simpleblog.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

import com.simpleblog.component.AccessKeyGenerator;
import com.simpleblog.dto.McpAccessKey;
import com.simpleblog.dto.UserSettingInfo;
import com.simpleblog.dto.UserSettingInput;
import com.simpleblog.entity.UserSetting;
import com.simpleblog.entity.UserSetting.LinkSetting;
import com.simpleblog.exception.InvalidUserSettingException;
import com.simpleblog.repository.UserSettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The personalization of a user: reading it (the defaults until it is saved), saving it, and renewing the MCP access key. */
@Service
public class UserSettingService {

	static final int NICK_MAX = 60;

	static final int THEME_MAX = 256;

	static final int LINK_NAME_MAX = 100;

	private static final Pattern SLUG = Pattern.compile("[A-Za-z0-9-]+");

	private final UserSettingRepository settingRepository;

	private final AccessKeyGenerator keyGenerator;

	private final Clock clock;

	private final int slugMaxLength;

	public UserSettingService(UserSettingRepository settingRepository, AccessKeyGenerator keyGenerator, Clock clock,
			@Value("${simpleblog.post.slug.max-length}") int slugMaxLength) {
		this.settingRepository = settingRepository;
		this.keyGenerator = keyGenerator;
		this.clock = clock;
		this.slugMaxLength = slugMaxLength;
	}

	/** The saved personalization, or the defaults with no modification time for a user who has saved nothing. */
	@Transactional(readOnly = true)
	public UserSettingInfo userSetting(long userId, String email) {
		return UserSettingInfo.from(email, this.settingRepository.findById(userId).orElseGet(() -> UserSetting.defaultsFor(userId)));
	}

	/** Saves the editable fields, creating the row at the first save. */
	@Transactional
	public UserSettingInfo update(long userId, String email, UserSettingInput input) {
		UserSetting setting = this.settingRepository.findById(userId).orElseGet(() -> UserSetting.defaultsFor(userId));
		setting.change(nickOf(input.nick()), requiredText(input.mainTheme(), "mainTheme", THEME_MAX),
				optionalText(input.subTheme(), "subTheme", THEME_MAX), link(input.aboutName(), input.aboutSlug(), "about"),
				link(input.workingName(), input.workingSlug(), "working"), now());
		return UserSettingInfo.from(email, this.settingRepository.save(setting));
	}

	/** Replaces the key with a new one and answers it; the old key stops working. */
	@Transactional
	public McpAccessKey renewAccessKey(long userId) {
		UserSetting setting = this.settingRepository.findById(userId).orElseGet(() -> UserSetting.defaultsFor(userId));
		setting.renewKey(this.keyGenerator.key(), now());
		return new McpAccessKey(this.settingRepository.save(setting).mcpAccessKey());
	}

	/** The nick name, or null when it is left out or blank. */
	private String nickOf(String nick) {
		String text = nick == null ? "" : nick.trim();
		if (text.length() > NICK_MAX) {
			throw new InvalidUserSettingException("nick is limited to " + NICK_MAX + " characters");
		}
		return text.isEmpty() ? null : text;
	}

	private LinkSetting link(String name, String slug, String field) {
		return new LinkSetting(requiredText(name, field + "Name", LINK_NAME_MAX), slugOf(slug, field + "Slug"));
	}

	private String requiredText(String value, String field, int max) {
		String text = optionalText(value, field, max);
		if (text.isEmpty()) {
			throw new InvalidUserSettingException(field + " is required");
		}
		return text;
	}

	private String optionalText(String value, String field, int max) {
		String text = value == null ? "" : value.trim();
		if (text.length() > max) {
			throw new InvalidUserSettingException(field + " is limited to " + max + " characters");
		}
		return text;
	}

	private String slugOf(String value, String field) {
		if (value == null || value.length() > this.slugMaxLength || !SLUG.matcher(value).matches()) {
			throw new InvalidUserSettingException(field + " must be English letters, digits and '-', up to " + this.slugMaxLength + " characters");
		}
		return value;
	}

	private LocalDateTime now() {
		return LocalDateTime.now(this.clock.withZone(ZoneOffset.UTC));
	}

}
