import { PostSummaryList } from '../component/PostSummaryList.tsx'
import { StatusMessage } from '../component/StatusMessage.tsx'
import type { PostQueryType } from '../model/Post.ts'
import { useListOfPosts } from '../model/useListOfPosts.ts'

interface PostSearchProps {
  queryType: PostQueryType
  query: string
}

/** The content area for /search: loads the matching posts and lists them. */
export function PostSearch({ queryType, query }: PostSearchProps) {
  const posts = useListOfPosts(queryType, query)
  switch (posts.state) {
    case 'loading':
      return <StatusMessage>검색 중…</StatusMessage>
    case 'failed':
      return <StatusMessage>검색 결과를 불러오지 못했습니다.</StatusMessage>
    case 'loaded':
      return (
        <PostSummaryList
          label="검색 결과"
          caption={<><q>{query}</q> 검색 결과 {posts.value.length}건</>}
          posts={posts.value}
        />
      )
  }
}
