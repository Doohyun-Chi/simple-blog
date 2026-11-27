import { PostListPage } from './PostListPage.tsx'

/** The reading area for /posts: the newest posts, with no search condition. */
export function PostList() {
  return <PostListPage title="글 목록" loadingText="불러오는 중…" failedText="글 목록을 불러오지 못했습니다." />
}
