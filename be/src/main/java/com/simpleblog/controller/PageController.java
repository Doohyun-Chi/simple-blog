package com.simpleblog.controller;

import com.simpleblog.dto.PageList;
import com.simpleblog.service.PageService;
import com.simpleblog.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/api/v1/pages")
public class PageController {

	private final PageService pageService;

	private final UserService userService;

	public PageController(PageService pageService, UserService userService) {
		this.pageService = pageService;
		this.userService = userService;
	}

	@GetMapping
	public PageList listOfPages(@AuthenticationPrincipal OidcUser user) {
		return this.pageService.listOfPages(this.userService.viewerId(user));
	}

}
