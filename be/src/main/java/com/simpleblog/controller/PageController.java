package com.simpleblog.controller;

import com.simpleblog.dto.PageList;
import com.simpleblog.service.PageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/api/v1/pages")
public class PageController {

	private final PageService pageService;

	public PageController(PageService pageService) {
		this.pageService = pageService;
	}

	@GetMapping
	public PageList listOfPages() {
		return this.pageService.listOfPages();
	}

}
