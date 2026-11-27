import type { ReactNode } from 'react'
import { PostSummaryList } from '../component/PostSummaryList.tsx'
import { StatusMessage } from '../component/StatusMessage.tsx'
import type { PostQueryType } from '../model/Post.ts'
import { useListOfPostPage } from '../model/useListOfPosts.ts'
import { usePostListPaging } from '../model/usePostListPaging.ts'

interface PostListPageProps {
  /** 글 목록 or 검색 목록. */
  title: string
  /** The search; none lists every post. */
  search?: { queryType: PostQueryType; query: string }
  condition?: ReactNode
  loadingText: string
  failedText: string
}

/** One page of the list view, as rows or as cards, with PREV and NEXT; it starts over at the first page when the search changes. */
export function PostListPage({ title, search, condition, loadingText, failedText }: PostListPageProps) {
  const paging = usePostListPaging(JSON.stringify(search ?? null))
  const page = useListOfPostPage({ ...search, offset: paging.offset, view: paging.view })
  switch (page.state) {
    case 'loading':
      return <StatusMessage>{loadingText}</StatusMessage>
    case 'failed':
      return <StatusMessage>{failedText}</StatusMessage>
    case 'loaded':
      return (
        <PostSummaryList title={title} condition={condition} posts={page.value.posts} view={paging.view} hasPrevious={paging.hasPrevious}
          hasNext={page.value.hasNext} onChooseView={paging.chooseView} onPrevious={paging.showPrevious} onNext={paging.showNext} />
      )
  }
}
