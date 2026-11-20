import { PostArticle } from '../component/PostArticle.tsx'
import { StatusMessage } from '../component/StatusMessage.tsx'
import { editPath } from '../model/route.ts'
import { shareUrl } from '../model/share.ts'
import { usePostBody } from '../model/usePostBody.ts'

interface PostReaderProps {
  slug: string
}

/**
 * The content area for /post/{slug} and /page/{slug}: loads the body and shows it. The 편집하기 link follows its `canEdit`; the
 * share buttons show for a published post only, as a draft has no share page.
 */
export function PostReader({ slug }: PostReaderProps) {
  const post = usePostBody(slug)
  switch (post.state) {
    case 'loading':
      return <StatusMessage>불러오는 중…</StatusMessage>
    case 'failed':
      return <StatusMessage>글을 불러오지 못했습니다.</StatusMessage>
    case 'loaded':
      return (
        <PostArticle
          post={post.value}
          editHref={post.value.canEdit ? editPath(slug) : undefined}
          shareHref={post.value.status === 'publish' ? shareUrl(slug) : undefined}
        />
      )
  }
}
