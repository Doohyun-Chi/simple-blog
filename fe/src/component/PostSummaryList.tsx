import type { ReactNode } from 'react'
import { dayOf, type PostSummary, readerSlug } from '../model/Post.ts'
import { contentPath } from '../model/route.ts'
import './PostSummaryList.css'

interface PostSummaryListProps {
  /** Names the list, for screen readers. */
  label: string
  /** The line above the list, such as the search query and the match count. */
  caption: ReactNode
  posts: PostSummary[]
}

/** A caption, then each post's title linking to its body, with its date. */
export function PostSummaryList({ label, caption, posts }: PostSummaryListProps) {
  return (
    <section className="post-summary-list" aria-label={label}>
      <p className="post-summary-list-caption">{caption}</p>
      <ol className="post-summary-list-items">
        {posts.map((post) => (
          <li key={post.id} className="post-summary-list-item">
            <a className="post-summary-list-link" href={contentPath('post', readerSlug(post.slug))}>{post.title}</a>
            <time className="post-summary-list-date" dateTime={post.date}>{dayOf(post.date)}</time>
          </li>
        ))}
      </ol>
    </section>
  )
}
