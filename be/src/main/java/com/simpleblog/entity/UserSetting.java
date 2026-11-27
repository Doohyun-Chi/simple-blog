package com.simpleblog.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The {@code TB_USER_SETTING} table of DBSCHEMA.md: how a user personalizes the blog. A user has a row from their first save
 * (or key renewal) on; before it {@link #defaultsFor} stands in. {@code MODIFIED_DATE} is UTC and null while it is only the defaults.
 */
@Entity
@Table(name = "TB_USER_SETTING")
public class UserSetting {

	public static final String DEFAULT_MAIN_THEME = "Dreaming for the Future";

	public static final String DEFAULT_SUB_THEME = "영원한 개발자를 향해";

	public static final String DEFAULT_ABOUT_NAME = "나의 꿈";

	public static final String DEFAULT_ABOUT_SLUG = "about";

	public static final String DEFAULT_WORKING_NAME = "제가 일하는 곳은?";

	public static final String DEFAULT_WORKING_SLUG = "my-working-places";

	@Id
	@Column(name = "USER_ID")
	private Long userId;

	@Column(name = "NICK")
	private String nick;

	@Column(name = "MAIN_THEME")
	private String mainTheme;

	@Column(name = "SUB_THEME")
	private String subTheme;

	@Column(name = "ABOUT_NAME")
	private String aboutName;

	@Column(name = "ABOUT_SLUG")
	private String aboutSlug;

	@Column(name = "WORKING_NAME")
	private String workingName;

	@Column(name = "WORKING_SLUG")
	private String workingSlug;

	@Column(name = "MCP_ACCESS_KEY")
	private String mcpAccessKey;

	@Column(name = "MODIFIED_DATE")
	private LocalDateTime modifiedDate;

	protected UserSetting() {
	}

	/** The personalization of a user who has saved nothing: every field at its default, no key and no modification time. */
	public static UserSetting defaultsFor(long userId) {
		UserSetting setting = new UserSetting();
		setting.userId = userId;
		setting.mainTheme = DEFAULT_MAIN_THEME;
		setting.subTheme = DEFAULT_SUB_THEME;
		setting.aboutName = DEFAULT_ABOUT_NAME;
		setting.aboutSlug = DEFAULT_ABOUT_SLUG;
		setting.workingName = DEFAULT_WORKING_NAME;
		setting.workingSlug = DEFAULT_WORKING_SLUG;
		return setting;
	}

	/** Replaces what the user may edit; the key stays. */
	public void change(String nick, String mainTheme, String subTheme, LinkSetting about, LinkSetting working, LocalDateTime utcNow) {
		this.nick = nick;
		this.mainTheme = mainTheme;
		this.subTheme = subTheme;
		this.aboutName = about.name();
		this.aboutSlug = about.slug();
		this.workingName = working.name();
		this.workingSlug = working.slug();
		this.modifiedDate = utcNow;
	}

	/** The display text and the post slug of one navigation link. */
	public record LinkSetting(String name, String slug) {
	}

	/** Puts a new MCP access key in place of the old one, which stops working. */
	public void renewKey(String key, LocalDateTime utcNow) {
		this.mcpAccessKey = key;
		this.modifiedDate = utcNow;
	}

	public Long userId() {
		return this.userId;
	}

	public String nick() {
		return this.nick;
	}

	public String mainTheme() {
		return this.mainTheme;
	}

	public String subTheme() {
		return this.subTheme;
	}

	public String aboutName() {
		return this.aboutName;
	}

	public String aboutSlug() {
		return this.aboutSlug;
	}

	public String workingName() {
		return this.workingName;
	}

	public String workingSlug() {
		return this.workingSlug;
	}

	public String mcpAccessKey() {
		return this.mcpAccessKey;
	}

	public LocalDateTime modifiedDate() {
		return this.modifiedDate;
	}

}
