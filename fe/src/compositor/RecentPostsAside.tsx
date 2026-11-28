import { RecentPostList } from '../component/RecentPostList.tsx'
import { StatusMessage } from '../component/StatusMessage.tsx'
import type { Bookmark } from '../model/Bookmark.ts'
import type { PostSummary } from '../model/Post.ts'
import type { Remote } from '../model/useRemote.ts'

interface RecentPostsAsideProps {
  recent: Remote<PostSummary[]>
  bookmarks: Remote<Bookmark[]>
  signedIn: boolean
  showAd: boolean
}

/** The info area on the right: the recent posts and the recent bookmarks, loaded once by the main page and shared with the home route. */
export function RecentPostsAside({ recent, bookmarks, signedIn, showAd }: RecentPostsAsideProps) {
  switch (recent.state) {
    case 'loading':
      return <StatusMessage>불러오는 중…</StatusMessage>
    case 'failed':
      return <StatusMessage>최근 글을 불러오지 못했습니다.</StatusMessage>
    case 'loaded':
      return <RecentPostList posts={recent.value} bookmarks={bookmarks} signedIn={signedIn} showAd={showAd} />
  }
}
