package com.simpleblog.controller;

import com.simpleblog.dto.PostBody;
import com.simpleblog.service.PostService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/api/v1/post")
public class PostController {

	private final PostService postService;

	public PostController(PostService postService) {
		this.postService = postService;
	}

	@GetMapping("/{slug}")
	public PostBody postBody(@PathVariable String slug) {
		return this.postService.postBody(slug);
	}

}
