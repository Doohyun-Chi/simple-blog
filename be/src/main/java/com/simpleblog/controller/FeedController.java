package com.simpleblog.controller;

import com.simpleblog.component.FeedDocument;
import com.simpleblog.component.ViewLinks;
import com.simpleblog.service.FeedService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** The Atom feed of the newest published posts. See be/apispec.yaml. */
@RestController
public class FeedController {

	private final FeedService feedService;

	private final FeedDocument feedDocument;

	private final ViewLinks viewLinks;

	public FeedController(FeedService feedService, FeedDocument feedDocument, ViewLinks viewLinks) {
		this.feedService = feedService;
		this.feedDocument = feedDocument;
		this.viewLinks = viewLinks;
	}

	@GetMapping(path = "/public/feed", produces = "application/atom+xml;charset=UTF-8")
	public String atomFeed() {
		String origin = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
		return this.feedDocument.render(this.feedService.latestFeed(), this.viewLinks.frontendOrigin(origin), origin + "/public/feed");
	}

}
