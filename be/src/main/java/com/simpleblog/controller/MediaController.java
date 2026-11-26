package com.simpleblog.controller;

import java.io.IOException;

import com.simpleblog.dto.ExternalMediaInput;
import com.simpleblog.dto.ImageUpload;
import com.simpleblog.dto.MediaFile;
import com.simpleblog.dto.MediaInfo;
import com.simpleblog.dto.MediaList;
import com.simpleblog.dto.MediaRename;
import com.simpleblog.service.MediaService;
import com.simpleblog.service.UserService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/public/api/v1")
public class MediaController {

	private final MediaService mediaService;

	private final UserService userService;

	public MediaController(MediaService mediaService, UserService userService) {
		this.mediaService = mediaService;
		this.userService = userService;
	}

	@GetMapping("/medias")
	public MediaList listOfMedias() {
		return this.mediaService.listOfMedias();
	}

	@PostMapping(path = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<MediaInfo> createImage(@AuthenticationPrincipal OidcUser user, @RequestPart("file") MultipartFile file,
			@RequestParam(name = "provider", required = false) String provider, @RequestParam(name = "slug", required = false) String slug,
			@RequestParam(name = "replace", defaultValue = "false") boolean replace) throws IOException {
		ImageUpload upload = new ImageUpload(file.getOriginalFilename(), file.getBytes(), provider, slug, replace);
		return responseOf(this.mediaService.uploadImage(this.userService.idOf(user), upload));
	}

	@PostMapping(path = "/media", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MediaInfo> createVideo(@AuthenticationPrincipal OidcUser user, @RequestBody ExternalMediaInput input) {
		return responseOf(this.mediaService.registerVideo(this.userService.idOf(user), input.url()));
	}

	private ResponseEntity<MediaInfo> responseOf(MediaService.Registration registration) {
		return ResponseEntity.status(registration.created() ? HttpStatus.CREATED : HttpStatus.OK).body(registration.media());
	}

	@GetMapping("/media/{slug}")
	public ResponseEntity<Resource> file(@PathVariable String slug) {
		MediaFile file = this.mediaService.file(slug);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(file.mimeType()))
			.cacheControl(CacheControl.noCache().cachePublic())
			.eTag("\"" + file.stored() + "\"")
			.body(file.resource());
	}

	@PutMapping("/media/{slug}")
	public MediaInfo rename(@AuthenticationPrincipal OidcUser user, @PathVariable String slug, @RequestBody MediaRename input) {
		return this.mediaService.rename(this.userService.idOf(user), slug, input.slug());
	}

	@DeleteMapping("/media/{slug}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal OidcUser user, @PathVariable String slug) {
		this.mediaService.delete(this.userService.idOf(user), slug);
	}

}
