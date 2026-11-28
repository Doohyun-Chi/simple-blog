import { IconFilePlus, IconList } from '@tabler/icons-react'
import { type ReactNode, useState } from 'react'
import type { Bookmark } from '../model/Bookmark.ts'
import { PostStatus, type PostSummary, PostType, readerSlug } from '../model/Post.ts'
import { contentPath, LIST_PATH, WRITE_PATH } from '../model/route.ts'
import type { Remote } from '../model/useRemote.ts'
import { AdSlot } from './AdSlot.tsx'
import { FeedLink } from './FeedLink.tsx'
import './RecentPostList.css'

interface RecentPostListProps {
  posts: PostSummary[]
  bookmarks: Remote<Bookmark[]>
  /** Whether the visitor is signed in; the link to the writing area and the bookmarks are for them only. */
  signedIn: boolean
  /** Whether a post is being read, which is the only time the info area shows an ad. */
  showAd: boolean
}

const TABS = { POSTS: 'posts', BOOKMARKS: 'bookmarks' } as const
type Tab = (typeof TABS)[keyof typeof TABS]

/** A title that links to a post, on one line. */
function TitleLink({ slug, title, children }: { slug: string; title: string; children?: ReactNode }) {
  return (
    <li>
      <a className="recent-post-list-link" href={contentPath(PostType.POST, readerSlug(slug))} title={title}>{children}{title}</a>
    </li>
  )
}

/** The titles of the recent posts; a draft is marked. */
function PostTitles({ posts }: { posts: PostSummary[] }) {
  return (
    <ul className="recent-post-list">
      {posts.map((post) => (
        <TitleLink key={post.id} slug={post.slug} title={post.title}>
          {post.status === PostStatus.DRAFT && <span className="recent-post-list-draft" aria-label="초안">* </span>}
        </TitleLink>
      ))}
    </ul>
  )
}

/** The names of the recent bookmarks; a note in the middle when there is nothing to list, or when the visitor must sign in first. */
function BookmarkTitles({ bookmarks, signedIn }: { bookmarks: Remote<Bookmark[]>; signedIn: boolean }) {
  if (!signedIn) {
    return <p className="recent-post-list-note">로그인 후 사용할 수 있습니다.</p>
  }
  if (bookmarks.state === 'loading') {
    return <p className="recent-post-list-note">불러오는 중…</p>
  }
  if (bookmarks.state === 'failed') {
    return <p className="recent-post-list-note">책갈피를 불러오지 못했습니다.</p>
  }
  if (bookmarks.value.length === 0) {
    return <p className="recent-post-list-note">아직 책갈피가 없습니다.</p>
  }
  return (
    <ul className="recent-post-list">
      {bookmarks.value.map((bookmark) => <TitleLink key={bookmark.id} slug={bookmark.slug} title={bookmark.name} />)}
    </ul>
  )
}

/** The two tabs above the list. */
function RecentTabs({ tab, onSelect }: { tab: Tab; onSelect: (tab: Tab) => void }) {
  return (
    <div className="recent-post-list-tabs" role="tablist">
      <button type="button" role="tab" className="recent-post-list-tab" aria-selected={tab === TABS.POSTS}
        onClick={() => onSelect(TABS.POSTS)}>최근 글</button>
      <button type="button" role="tab" className="recent-post-list-tab" aria-selected={tab === TABS.BOOKMARKS}
        onClick={() => onSelect(TABS.BOOKMARKS)}>최근 책갈피</button>
    </div>
  )
}

/**
 * The info area's list: the tabs with the titles of the recent posts or the names of the recent bookmarks, in a box as high as seven
 * titles, then the link to all posts with the feed logo beside it, and, for a signed-in visitor, the link to the writing area.
 */
export function RecentPostList({ posts, bookmarks, signedIn, showAd }: RecentPostListProps) {
  const [tab, updateTab] = useState<Tab>(TABS.POSTS)
  return (
    <>
      <RecentTabs tab={tab} onSelect={updateTab} />
      <div className="recent-post-list-panel" role="tabpanel">
        {tab === TABS.POSTS ? <PostTitles posts={posts} /> : <BookmarkTitles bookmarks={bookmarks} signedIn={signedIn} />}
      </div>
      <ul className="recent-post-list recent-post-list-links">
        <li className="recent-post-list-all-row">
          <a className="recent-post-list-link recent-post-list-all" href={LIST_PATH}>
            <IconList className="recent-post-list-icon" size="1em" aria-hidden="true" /> 전체 글 목록
          </a>
          <FeedLink />
        </li>
        {signedIn && (
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
