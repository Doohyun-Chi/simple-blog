package com.simpleblog.controller;

import com.simpleblog.dto.McpAccessKey;
import com.simpleblog.dto.UserSettingInfo;
import com.simpleblog.dto.UserSettingInput;
import com.simpleblog.service.UserService;
import com.simpleblog.service.UserSettingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The personalization API of be/apispec.yaml. Security only lets a signed-in user reach it. */
@RestController
@RequestMapping("/public/api/v1/user")
public class UserSettingController {

	private final UserSettingService settingService;

	private final UserService userService;

	public UserSettingController(UserSettingService settingService, UserService userService) {
		this.settingService = settingService;
		this.userService = userService;
	}

	@GetMapping
	public UserSettingInfo userSetting(@AuthenticationPrincipal OidcUser user) {
		return this.settingService.userSetting(this.userService.idOf(user), user.getEmail());
	}

	@PutMapping
	public UserSettingInfo updateUserSetting(@AuthenticationPrincipal OidcUser user, @RequestBody UserSettingInput input) {
		return this.settingService.update(this.userService.idOf(user), user.getEmail(), input);
	}

	@PutMapping("/access-token")
	public McpAccessKey renewMcpAccessKey(@AuthenticationPrincipal OidcUser user) {
		return this.settingService.renewAccessKey(this.userService.idOf(user));
	}

}
