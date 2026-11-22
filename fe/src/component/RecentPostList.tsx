import { IconFilePlus } from '@tabler/icons-react'
import { PostStatus, type PostSummary, PostType, readerSlug } from '../model/Post.ts'
import { contentPath, LIST_PATH, WRITE_PATH } from '../model/route.ts'
import { AdSlot } from './AdSlot.tsx'
import { FeedLink } from './FeedLink.tsx'
import './RecentPostList.css'

interface RecentPostListProps {
  posts: PostSummary[]
  /** Whether the visitor is signed in; the link to the writing area is shown only then. */
  canWrite: boolean
  /** Whether a post is being read, which is the only time the info area shows an ad. */
  showAd: boolean
}

/**
 * The info area's list: only the titles, each linking to its body, then the link to all posts with the feed logo beside it,
 * and the link to the writing area.
 */
export function RecentPostList({ posts, canWrite, showAd }: RecentPostListProps) {
  return (
    <>
      <h2 className="recent-post-list-heading">최근 글</h2>
      <ul className="recent-post-list">
        {posts.map((post) => (
          <li key={post.id}>
            <a
              className="recent-post-list-link"
              href={contentPath(PostType.POST, readerSlug(post.slug))}
              title={post.title}
            >
              {post.status === PostStatus.DRAFT && <span className="recent-post-list-draft" aria-label="초안">* </span>}
              {post.title}
            </a>
          </li>
        ))}
        <li className="recent-post-list-all-row">
          <a className="recent-post-list-link recent-post-list-all" href={LIST_PATH}>전체 목록</a>
          <FeedLink />
        </li>
        {canWrite && (
          <li>
            <a className="recent-post-list-link recent-post-list-all" href={WRITE_PATH}>
              <IconFilePlus className="recent-post-list-icon" size="1em" aria-hidden="true" /> 새로운 생각
            </a>
          </li>
        )}
      </ul>
      {showAd && <AdSlot position="aside" />}
    </>
  )
}
