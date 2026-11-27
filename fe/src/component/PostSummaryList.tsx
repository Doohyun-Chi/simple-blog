import type { ReactNode } from 'react'
import { type PostListView, type PostSummary, PostListView as Views } from '../model/Post.ts'
import { PostListPager } from './PostListPager.tsx'
import { PostListViewToggle } from './PostListViewToggle.tsx'
import { PostSummaryCards } from './PostSummaryCards.tsx'
import { PostSummaryRows } from './PostSummaryRows.tsx'
import './PostSummaryList.css'

interface PostSummaryListProps {
  /** 글 목록 or 검색 목록. */
  title: string
  /** The search condition, shown under the title; none for the list of all posts. */
  condition?: ReactNode
  posts: PostSummary[]
  view: PostListView
  hasPrevious: boolean
  hasNext: boolean
  onChooseView: (view: PostListView) => void
  onPrevious: () => void
  onNext: () => void
}

/** The list view: its title with the choice of the view, the posts as rows or cards, and PREV and NEXT. */
export function PostSummaryList({ title, condition, posts, view, hasPrevious, hasNext, onChooseView, onPrevious, onNext }: PostSummaryListProps) {
  return (
    <section className="post-summary-list">
      <div className="post-summary-list-bar">
        <h2 className="post-summary-list-heading">{title}</h2>
        <PostListViewToggle view={view} onChoose={onChooseView} />
      </div>
      {condition !== undefined && <p className="post-summary-list-condition">{condition}</p>}
      <div className="post-summary-list-body">
        {view === Views.CARD ? <PostSummaryCards posts={posts} /> : <PostSummaryRows posts={posts} />}
      </div>
      <PostListPager hasPrevious={hasPrevious} hasNext={hasNext} onPrevious={onPrevious} onNext={onNext} />
    </section>
  )
}
