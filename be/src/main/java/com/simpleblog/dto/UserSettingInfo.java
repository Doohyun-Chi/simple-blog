package com.simpleblog.dto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.simpleblog.entity.UserSetting;

/** The personalization of the signed-in user as in be/apispec.yaml ({@code UserSetting}). */
public record UserSettingInfo(String email, String nick, String mainTheme, String subTheme, String aboutName, String aboutSlug,
		String workingName, String workingSlug, String mcpAccessKey, OffsetDateTime modifiedDate) {

	public static UserSettingInfo from(String email, UserSetting setting) {
		OffsetDateTime modified = setting.modifiedDate() == null ? null : setting.modifiedDate().atOffset(ZoneOffset.UTC);
		return new UserSettingInfo(email, setting.nick(), setting.mainTheme(), setting.subTheme(), setting.aboutName(), setting.aboutSlug(),
				setting.workingName(), setting.workingSlug(), setting.mcpAccessKey(), modified);
	}

}
