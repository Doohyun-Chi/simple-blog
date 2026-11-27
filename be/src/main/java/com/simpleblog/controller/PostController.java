package com.simpleblog.controller;

import com.simpleblog.dto.PostBody;
import com.simpleblog.dto.PostInput;
import com.simpleblog.dto.PostList;
import com.simpleblog.dto.PostListRequest;
import com.simpleblog.dto.PostListView;
import com.simpleblog.dto.PostQueryType;
import com.simpleblog.service.PostListService;
import com.simpleblog.service.PostService;
import com.simpleblog.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/public/api/v1")
public class PostController {

	private final PostService postService;

	private final PostListService postListService;

	private final UserService userService;

	public PostController(PostService postService, PostListService postListService, UserService userService) {
		this.postService = postService;
		this.postListService = postListService;
		this.userService = userService;
	}

	@GetMapping("/posts")
	public PostList listOfPosts(@AuthenticationPrincipal OidcUser user, @RequestParam(defaultValue = "keyword") String queryType,
			@RequestParam(required = false) String query, @RequestParam(required = false) Integer limit,
			@RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "list") String view) {
		String origin = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
		PostListRequest request = new PostListRequest(PostQueryType.of(queryType), query, limit, offset, PostListView.of(view), origin);
		return this.postListService.listOfPosts(this.userService.viewerId(user), request);
	}

	@GetMapping("/post/{slug}")
	public PostBody postBody(@PathVariable String slug) {
		return this.postService.postBody(slug);
	}

	@PostMapping("/post")
	@ResponseStatus(HttpStatus.CREATED)
	public PostBody createPost(@RequestBody PostInput input) {
		return this.postService.createPost(input);
	}

	@DeleteMapping("/post/{slug}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deletePost(@PathVariable String slug) {
		this.postService.deletePost(slug);
	}

	@PutMapping("/post/{slug}")
	public PostBody updatePost(@PathVariable String slug, @RequestBody PostInput input) {
		return this.postService.updatePost(slug, input);
	}

}
