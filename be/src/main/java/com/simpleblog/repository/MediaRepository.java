package com.simpleblog.repository;

import java.util.Optional;

import com.simpleblog.entity.Media;
import org.springframework.data.repository.Repository;

/** Only saving and looking up by slug: a media row is never changed or deleted through the API. */
public interface MediaRepository extends Repository<Media, Long> {

	Media save(Media media);

	Optional<Media> findBySlug(String slug);

}
