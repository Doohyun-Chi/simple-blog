import type { ReactNode } from 'react'
import { dayOf, type PostSummary, readerSlug } from '../model/Post.ts'
import { contentPath } from '../model/route.ts'
import './PostSummaryList.css'

interface PostSummaryListProps {
  /** The search condition or the list's name. It stays at the top while the list scrolls. */
  heading: ReactNode
  posts: PostSummary[]
}

/** The list view: a heading, then each post's title linking to its body, with its date. */
export function PostSummaryList({ heading, posts }: PostSummaryListProps) {
  return (
    <section className="post-summary-list">
      <h2 className="post-summary-list-heading">{heading}</h2>
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
