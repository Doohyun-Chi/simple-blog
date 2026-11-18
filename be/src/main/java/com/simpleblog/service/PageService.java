package com.simpleblog.service;

import com.simpleblog.dto.PageList;
import com.simpleblog.dto.PageSummary;
import com.simpleblog.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PageService {

	private final PostRepository postRepository;

	public PageService(PostRepository postRepository) {
		this.postRepository = postRepository;
	}

	@Transactional(readOnly = true)
	/** The published pages, plus every page of the viewer (see UserService#viewerId) whatever its status. */
	public PageList listOfPages(long viewerId) {
		return new PageList(this.postRepository.listOfPagesVisibleTo(viewerId).stream().map(PageSummary::from).toList());
	}

}
