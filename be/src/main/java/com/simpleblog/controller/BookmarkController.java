package com.simpleblog.controller;

import com.simpleblog.dto.BookmarkArrangement;
import com.simpleblog.dto.BookmarkInfo;
import com.simpleblog.dto.BookmarkInput;
import com.simpleblog.dto.BookmarkList;
import com.simpleblog.dto.BookmarkRename;
import com.simpleblog.service.BookmarkService;
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

@RestController
@RequestMapping("/public/api/v1")
public class BookmarkController {

	private final BookmarkService bookmarkService;

	private final UserService userService;

	public BookmarkController(BookmarkService bookmarkService, UserService userService) {
		this.bookmarkService = bookmarkService;
		this.userService = userService;
	}

	@GetMapping("/bookmarks")
	public BookmarkList listOfBookmarks(@AuthenticationPrincipal OidcUser user, @RequestParam(required = false) Integer limit) {
		return this.bookmarkService.listOfBookmarks(this.userService.idOf(user), limit);
	}

	@PutMapping("/bookmarks")
	public BookmarkList saveBookmarks(@AuthenticationPrincipal OidcUser user, @RequestBody BookmarkArrangement input) {
		return this.bookmarkService.save(this.userService.idOf(user), input);
	}

	@PostMapping("/bookmark")
	@ResponseStatus(HttpStatus.CREATED)
	public BookmarkInfo createBookmark(@AuthenticationPrincipal OidcUser user, @RequestBody BookmarkInput input) {
		return this.bookmarkService.createBookmark(this.userService.idOf(user), input.slug());
	}

	@PutMapping("/bookmark/{id}")
	public BookmarkInfo renameBookmark(@AuthenticationPrincipal OidcUser user, @PathVariable long id, @RequestBody BookmarkRename input) {
		return this.bookmarkService.rename(this.userService.idOf(user), id, input.name());
	}

	@DeleteMapping("/bookmark/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteBookmark(@AuthenticationPrincipal OidcUser user, @PathVariable long id) {
		this.bookmarkService.delete(this.userService.idOf(user), id);
	}

}
