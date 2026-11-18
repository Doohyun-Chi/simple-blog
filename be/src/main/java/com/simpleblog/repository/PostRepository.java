package com.simpleblog.repository;

import java.util.List;
import java.util.Optional;

import com.simpleblog.dto.PostSummary;
import com.simpleblog.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Extends {@link Repository} rather than JpaRepository on purpose, so the only writes are {@link #save} and
 * {@link #delete}, and nothing can delete rows in bulk.
 */
public interface PostRepository extends Repository<Post, Long> {

	/** The published pages and, whatever their status, the pages of this viewer. */
	@Query("select p from Post p where p.type = 'page' and (p.status = 'publish' or p.authorId = :viewerId) order by p.id")
	List<Post> listOfPagesVisibleTo(long viewerId);

	/**
	 * Published posts, and whatever their status the posts of this viewer, whose title or content matches the LIKE pattern, newest first. The pattern must be lowercase and
	 * escaped with {@code \}; see LikePattern. The pageable only limits the count; the order is fixed. Selects summaries
	 * only, so post content is never loaded; the date is UTC, see PostSummary#inZone.
	 */
	@Query("""
			select new com.simpleblog.dto.PostSummary(p.id, p.title, p.slug, p.status, p.createdAt) from Post p
			where p.type = 'post' and (p.status = 'publish' or p.authorId = :viewerId)
			  and (lower(p.title) like :pattern escape '\\' or lower(p.content) like :pattern escape '\\')
			order by p.createdAt desc, p.id desc""")
	List<PostSummary> listOfPostsContaining(String pattern, long viewerId, Pageable pageable);

	Post save(Post post);

	/** Removes the row. Only PostService#deletePost calls it, for a row of type post. */
	void delete(Post post);

	/** Whether any row, of any type or status, already has this stored slug. */
	@Query("select count(p) > 0 from Post p where p.slug = :slug")
	boolean slugInUse(String slug);

	/** Whether a row other than this one has this stored slug. */
	@Query("select count(p) > 0 from Post p where p.slug = :slug and p.id <> :id")
	boolean slugInUseExcept(String slug, Long id);

	/** The post to update: only rows of type post can be written, never pages, attachments or revisions. */
	@Query("select p from Post p where p.slug = :slug and p.type = 'post'")
	Optional<Post> writablePost(String slug);

	/** The row with this slug, of any type or status. SLUG is unique across all of them, so there is at most one. */
	@Query("select p from Post p where p.slug = :slug")
	Optional<Post> post(String slug);

}
