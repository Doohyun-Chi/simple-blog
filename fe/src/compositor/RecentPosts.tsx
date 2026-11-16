import { PostSummaryList } from '../component/PostSummaryList.tsx'
import { StatusMessage } from '../component/StatusMessage.tsx'
import { useListOfRecentPosts } from '../model/useListOfPosts.ts'

/** The content area for the home route: the newest published posts, the same ones a search could find. */
export function RecentPosts() {
  const posts = useListOfRecentPosts()
  switch (posts.state) {
    case 'loading':
      return <StatusMessage>불러오는 중…</StatusMessage>
    case 'failed':
      return <StatusMessage>최근 글을 불러오지 못했습니다.</StatusMessage>
    case 'loaded':
      return <PostSummaryList label="최근 글" caption="최근 글" posts={posts.value} />
  }
}
