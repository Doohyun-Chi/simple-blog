package com.simpleblog.dto;

/** The JSON body of the personalization update in be/apispec.yaml ({@code UserSettingInput}). */
public record UserSettingInput(String nick, String mainTheme, String subTheme, String aboutSlug, String workingSlug) {
}
