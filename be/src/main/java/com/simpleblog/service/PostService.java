package com.simpleblog.service;

import com.simpleblog.component.SlugEncoder;
import com.simpleblog.dto.PostBody;
import com.simpleblog.exception.PostNotFoundException;
import com.simpleblog.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

	private final PostRepository postRepository;

	private final SlugEncoder slugEncoder;

	public PostService(PostRepository postRepository, SlugEncoder slugEncoder) {
		this.postRepository = postRepository;
		this.slugEncoder = slugEncoder;
	}

	@Transactional(readOnly = true)
	public PostBody postBody(String slug) {
		return this.postRepository.post(this.slugEncoder.encode(slug))
			.map(PostBody::from)
			.orElseThrow(() -> new PostNotFoundException(slug));
	}

}
