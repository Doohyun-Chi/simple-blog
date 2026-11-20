package com.simpleblog.controller;

import com.simpleblog.component.SharePage;
import com.simpleblog.service.ShareService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** The page link-preview crawlers read instead of the post view, which is rendered by script. See be/apispec.yaml. */
@RestController
@RequestMapping("/public/share")
public class ShareController {

	private final ShareService shareService;

	private final SharePage sharePage;

	public ShareController(ShareService shareService, SharePage sharePage) {
		this.shareService = shareService;
		this.sharePage = sharePage;
	}

	@GetMapping(path = "/{slug}", produces = "text/html;charset=UTF-8")
	public String shareOfPost(@PathVariable String slug) {
		String origin = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
		return this.sharePage.render(this.shareService.shareOfPost(slug, origin));
	}

}
