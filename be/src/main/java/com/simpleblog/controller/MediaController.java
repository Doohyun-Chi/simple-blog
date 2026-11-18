package com.simpleblog.controller;

import java.io.IOException;
import java.time.Duration;

import com.simpleblog.dto.ExternalMediaInput;
import com.simpleblog.dto.MediaFile;
import com.simpleblog.dto.MediaInfo;
import com.simpleblog.service.MediaService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/public/api/v1/media")
public class MediaController {

	private final MediaService mediaService;

	public MediaController(MediaService mediaService) {
		this.mediaService = mediaService;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<MediaInfo> createImage(@RequestPart("file") MultipartFile file) throws IOException {
		return ResponseEntity.status(HttpStatus.CREATED).body(this.mediaService.uploadImage(file.getOriginalFilename(), file.getBytes()));
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MediaInfo> createVideo(@RequestBody ExternalMediaInput input) {
		MediaService.Registration registration = this.mediaService.registerVideo(input.url());
		return ResponseEntity.status(registration.created() ? HttpStatus.CREATED : HttpStatus.OK).body(registration.media());
	}

	@GetMapping("/{slug}")
	public ResponseEntity<Resource> file(@PathVariable String slug) {
		MediaFile file = this.mediaService.file(slug);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(file.mimeType()))
			.cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
			.body(file.resource());
	}

}
