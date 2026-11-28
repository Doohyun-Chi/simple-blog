package com.simpleblog.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.simpleblog.entity.Bookmark;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** The bookmarks of the users. A lookup always names the owner, so nobody reaches another user's bookmark. */
public interface BookmarkRepository extends Repository<Bookmark, Long> {

	Bookmark save(Bookmark bookmark);

	void delete(Bookmark bookmark);

	Optional<Bookmark> findByIdAndOwnerId(Long id, Long ownerId);

	Optional<Bookmark> findByOwnerIdAndPostId(Long ownerId, Long postId);

	boolean existsByOwnerIdAndPostId(Long ownerId, Long postId);

	/** The owner's bookmarks, the pinned ones first by the larger order, then the newest first. */
	@Query("select b from Bookmark b where b.ownerId = :ownerId order by b.sortOrder desc, b.id desc")
	List<Bookmark> listOfBookmarksOf(long ownerId, Pageable pageable);

	/** Those of the owner's bookmarks that have one of the ids, in the order of {@link #listOfBookmarksOf}. */
	@Query("select b from Bookmark b where b.ownerId = :ownerId and b.id in :ids order by b.sortOrder desc, b.id desc")
	List<Bookmark> listOfBookmarksIn(long ownerId, Collection<Long> ids);

	/** Gives the bookmarks of a post the slug it has been renamed to. */
	@Modifying
	@Query("update Bookmark b set b.postSlug = :slug where b.postId = :postId")
	void follow(long postId, String slug);

	/** Removes the bookmarks of a post that is deleted. */
	@Modifying
	@Query("delete from Bookmark b where b.postId = :postId")
	void deleteOfPost(long postId);

}
