import { StatusMessage } from '../component/StatusMessage.tsx'
import { PostStatus, type SettablePostStatus } from '../model/Post.ts'
import { usePostBody } from '../model/usePostBody.ts'
import { PostWriter } from './PostWriter.tsx'

/** The stored status as the editor knows it: only publish matters, anything else (draft, private) can be published. */
function statusOf(status: PostStatus): SettablePostStatus {
  return status === PostStatus.PUBLISH ? PostStatus.PUBLISH : PostStatus.DRAFT
}

interface PostReviserProps {
  slug: string
}

/** The content area for /edit/{slug}: loads the post and opens the editor with it. */
export function PostReviser({ slug }: PostReviserProps) {
  const post = usePostBody(slug)
  switch (post.state) {
    case 'loading':
      return <StatusMessage>불러오는 중…</StatusMessage>
    case 'failed':
      return <StatusMessage>글을 불러오지 못했습니다.</StatusMessage>
    case 'loaded':
      if (!post.value.canEdit) {
        return <StatusMessage>이 글을 편집할 권한이 없습니다.</StatusMessage>
      }
      return <PostWriter
          initial={{ title: post.value.title, slug, content: post.value.content, status: statusOf(post.value.status), hashTags: post.value.hashTags }}
          contentType={post.value.type}
        />
  }
}
