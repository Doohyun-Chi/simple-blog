package com.simpleblog.controller;

import com.simpleblog.dto.PostBody;
import com.simpleblog.dto.PostList;
import com.simpleblog.dto.PostQueryType;
import com.simpleblog.service.PostService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/api/v1")
public class PostController {

	private final PostService postService;

	public PostController(PostService postService) {
		this.postService = postService;
	}

	@GetMapping("/posts")
	public PostList listOfPosts(@RequestParam(defaultValue = "keyword") String queryType, @RequestParam String query) {
		return this.postService.listOfPosts(PostQueryType.of(queryType), query);
	}

	@GetMapping("/post/{slug}")
	public PostBody postBody(@PathVariable String slug) {
		return this.postService.postBody(slug);
	}

}
