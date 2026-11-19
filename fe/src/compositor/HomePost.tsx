import { StatusMessage } from '../component/StatusMessage.tsx'
import { type PostSummary, readerSlug } from '../model/Post.ts'
import type { Remote } from '../model/useRemote.ts'
import { PostReader } from './PostReader.tsx'

interface HomePostProps {
  recent: Remote<PostSummary[]>
}

/** The reading area for the home route: the body of the newest post (fe/DESIGN.md). */
export function HomePost({ recent }: HomePostProps) {
  switch (recent.state) {
    case 'loading':
      return <StatusMessage>불러오는 중…</StatusMessage>
    case 'failed':
      return <StatusMessage>글을 불러오지 못했습니다.</StatusMessage>
    case 'loaded':
      return recent.value.length === 0 ? null : <PostReader slug={readerSlug(recent.value[0].slug)} />
  }
}
