import type { PostQueryType } from '../model/Post.ts'
import { PostListPage } from './PostListPage.tsx'

interface PostSearchProps {
  queryType: PostQueryType
  query: string
}

/** The content area for /search: loads the matching posts and lists them. */
export function PostSearch({ queryType, query }: PostSearchProps) {
  return (
    <PostListPage title="검색 목록" search={{ queryType, query }} condition={<><q>{query}</q> 검색 결과</>} loadingText="검색 중…"
      failedText="검색 결과를 불러오지 못했습니다." />
  )
}
