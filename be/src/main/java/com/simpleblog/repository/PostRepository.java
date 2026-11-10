package com.simpleblog.repository;

import java.util.List;
import java.util.Optional;

import com.simpleblog.entity.Post;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Read-only on purpose: extends {@link Repository} rather than JpaRepository so no save or delete methods exist.
 */
public interface PostRepository extends Repository<Post, Long> {

	@Query("select p from Post p where p.type = 'page' and p.status = 'publish' order by p.menuOrder, p.id")
	List<Post> listOfPublishedPages();

	/** The row with this slug, of any type, status or password. post_name is not unique across types. */
	@Query("select p from Post p where p.slug = :slug")
	Optional<Post> post(String slug);

}
