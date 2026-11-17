import { RecentPostList } from '../component/RecentPostList.tsx'
import { StatusMessage } from '../component/StatusMessage.tsx'
import type { PostSummary } from '../model/Post.ts'
import type { Remote } from '../model/useRemote.ts'

interface RecentPostsAsideProps {
  recent: Remote<PostSummary[]>
}

/** The info area on the right: the recent posts, loaded once by the main page and shared with the home route. */
export function RecentPostsAside({ recent }: RecentPostsAsideProps) {
  switch (recent.state) {
    case 'loading':
      return <StatusMessage>불러오는 중…</StatusMessage>
    case 'failed':
      return <StatusMessage>최근 글을 불러오지 못했습니다.</StatusMessage>
    case 'loaded':
      return <RecentPostList posts={recent.value} />
  }
}
