package com.simpleblog.repository;

import java.util.List;
import java.util.Optional;

import com.simpleblog.dto.PostSummary;
import com.simpleblog.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Read-only on purpose: extends {@link Repository} rather than JpaRepository so no save or delete methods exist.
 */
public interface PostRepository extends Repository<Post, Long> {

	@Query("select p from Post p where p.type = 'page' and p.status = 'publish' order by p.menuOrder, p.id")
	List<Post> listOfPublishedPages();

	/**
	 * Published, unprotected posts whose title or content matches the LIKE pattern, newest first. The pattern must be
	 * lowercase and escaped with {@code \}; see LikePattern. The pageable only limits the count; the order is fixed.
	 * Selects summaries only, so post content is never loaded.
	 */
	@Query("""
			select new com.simpleblog.dto.PostSummary(p.id, p.title, p.slug, p.publishedAtLocal) from Post p
			where p.type = 'post' and p.status = 'publish' and p.password = ''
			  and (lower(p.title) like :pattern escape '\\' or lower(p.content) like :pattern escape '\\')
			order by p.publishedAtLocal desc, p.id desc""")
	List<PostSummary> listOfPublishedPostsContaining(String pattern, Pageable pageable);

	/** The row with this slug, of any type, status or password. post_name is not unique across types. */
	@Query("select p from Post p where p.slug = :slug")
	Optional<Post> post(String slug);

}
