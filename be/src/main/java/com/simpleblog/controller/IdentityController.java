package com.simpleblog.controller;

import com.simpleblog.dto.Identity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/api/v1")
public class IdentityController {

	/** Security only lets a signed-in user reach this, so the principal is always there. */
	@GetMapping("/me")
	public Identity me(@AuthenticationPrincipal OidcUser user) {
		return new Identity(user.getEmail());
	}

}
