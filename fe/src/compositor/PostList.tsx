import { PostSummaryList } from '../component/PostSummaryList.tsx'
import { StatusMessage } from '../component/StatusMessage.tsx'
import { useListOfAllPosts } from '../model/useListOfPosts.ts'

/** The reading area for /posts: the newest posts, with no search condition. */
export function PostList() {
  const posts = useListOfAllPosts()
  switch (posts.state) {
    case 'loading':
      return <StatusMessage>불러오는 중…</StatusMessage>
    case 'failed':
      return <StatusMessage>글 목록을 불러오지 못했습니다.</StatusMessage>
    case 'loaded':
      return <PostSummaryList heading="전체 목록" posts={posts.value} />
  }
}
