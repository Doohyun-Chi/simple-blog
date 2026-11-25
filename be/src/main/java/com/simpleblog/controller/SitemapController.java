package com.simpleblog.controller;

import com.simpleblog.component.SitemapDocument;
import com.simpleblog.component.ViewLinks;
import com.simpleblog.service.SitemapService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** The sitemap of the published posts. See be/apispec.yaml. */
@RestController
public class SitemapController {

	private final SitemapService sitemapService;

	private final SitemapDocument sitemapDocument;

	private final ViewLinks viewLinks;

	public SitemapController(SitemapService sitemapService, SitemapDocument sitemapDocument, ViewLinks viewLinks) {
		this.sitemapService = sitemapService;
		this.sitemapDocument = sitemapDocument;
		this.viewLinks = viewLinks;
	}

	@GetMapping(path = "/public/sitemap.xml", produces = "application/xml;charset=UTF-8")
	public String sitemap() {
		String origin = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
		return this.sitemapDocument.render(this.sitemapService.currentSitemap(), this.viewLinks.frontendOrigin(origin));
	}

}
