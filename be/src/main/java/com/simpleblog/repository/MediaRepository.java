package com.simpleblog.repository;

import java.util.List;
import java.util.Optional;

import com.simpleblog.entity.Media;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Saving, looking up and deleting one row: a media row is never changed, and only MediaService#discardUnused deletes one,
 * an image a post save replaced.
 */
public interface MediaRepository extends Repository<Media, Long> {

	Media save(Media media);

	Optional<Media> findBySlug(String slug);

	void delete(Media media);

	/** Every item, newest first. */
	@Query("select m from Media m order by m.date desc, m.id desc")
	List<Media> listOfMedias();

}
