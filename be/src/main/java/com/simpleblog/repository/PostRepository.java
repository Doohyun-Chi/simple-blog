package com.simpleblog.repository;

import java.util.List;
import java.util.Optional;

import com.simpleblog.dto.FeedRow;
import com.simpleblog.dto.SitemapRow;
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

	/**
	 * Published posts, and whatever their status the posts of this viewer, whose title or content matches the LIKE pattern, newest first. The pattern must be lowercase and
	 * escaped with {@code \}; see LikePattern. The pageable only limits the count; the order is fixed. Selects summaries
	 * only, so post content is never loaded; the date is UTC, see PostSummary#inZone.
	 */
	@Query("""
			select new com.simpleblog.dto.PostSummary(p.id, p.title, p.slug, p.status, p.createdAt) from Post p
			where p.type = com.simpleblog.entity.PostType.POST
			  and (p.status = com.simpleblog.entity.PostStatus.PUBLISH or p.authorId = :viewerId)
			  and (lower(p.title) like :pattern escape '\\' or lower(p.content) like :pattern escape '\\')
			order by p.createdAt desc, p.id desc""")
	List<PostSummary> listOfPostsContaining(String pattern, long viewerId, Pageable pageable);

	/** The newest published posts for the feed, whoever asks: never a draft, a private post or a page. The pageable only limits the count. */
	@Query("""
			select new com.simpleblog.dto.FeedRow(p.title, p.slug, p.createdAt, p.modifiedAt) from Post p
			where p.type = com.simpleblog.entity.PostType.POST and p.status = com.simpleblog.entity.PostStatus.PUBLISH
			order by p.createdAt desc, p.id desc""")
	List<FeedRow> listOfFeedRows(Pageable pageable);

	/** Every published post for the sitemap, whoever asks: never a draft, a private post or a page. Newest modification first. */
	@Query("""
			select new com.simpleblog.dto.SitemapRow(p.slug, p.modifiedAt) from Post p
			where p.type = com.simpleblog.entity.PostType.POST and p.status = com.simpleblog.entity.PostStatus.PUBLISH
			order by p.modifiedAt desc, p.id desc""")
	List<SitemapRow> listOfSitemapRows();

	Post save(Post post);

	/** Removes the row. Only PostService#deletePost calls it. */
	void delete(Post post);

	/** Whether any row, of any type or status, already has this stored slug. */
	@Query("select count(p) > 0 from Post p where p.slug = :slug")
	boolean slugInUse(String slug);

	/** Whether a row other than this one has this stored slug. */
	@Query("select count(p) > 0 from Post p where p.slug = :slug and p.id <> :id")
	boolean slugInUseExcept(String slug, Long id);

	/**
	 * The content of every post and page, of any status, whose content matches the LIKE pattern, lowercased and escaped
	 * with {@code \}; see LikePattern.
	 */
	@Query("select p.content from Post p where lower(p.content) like :pattern escape '\\'")
	List<String> listOfContentsContaining(String pattern);

	/** The post or page with this slug, of any status. SLUG is unique across all of them, so there is at most one. */
	@Query("select p from Post p where p.slug = :slug")
	Optional<Post> post(String slug);

}
