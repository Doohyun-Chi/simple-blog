import { dayOf, type PostSummary, PostType, readerSlug, statusLabelOf } from '../model/Post.ts'
import { contentPath } from '../model/route.ts'
import './PostSummaryRows.css'

/** The posts as rows: each title linking to its body, with its status tag and its date. */
export function PostSummaryRows({ posts }: { posts: PostSummary[] }) {
  return (
    <ol className="post-summary-rows">
      {posts.map((post) => (
        <li key={post.id} className="post-summary-row">
          <a className="post-summary-row-link" href={contentPath(PostType.POST, readerSlug(post.slug))}>{post.title}</a>
          {statusLabelOf(post.status) !== null && <span className="post-summary-row-tag">{statusLabelOf(post.status)}</span>}
          <time className="post-summary-row-date" dateTime={post.date}>{dayOf(post.date)}</time>
        </li>
      ))}
    </ol>
  )
}
