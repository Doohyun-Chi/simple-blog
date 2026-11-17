import { lazy, Suspense } from 'react'
import { StatusMessage } from '../component/StatusMessage.tsx'

/** The MDX editor is large, so it loads only when the writing area is opened and reading pages stay light. */
const PostEditor = lazy(() => import('../component/PostEditor.tsx').then((module) => ({ default: module.PostEditor })))

/** The content area for /write. */
export function PostWriter() {
  return (
    <Suspense fallback={<StatusMessage>편집기를 불러오는 중…</StatusMessage>}>
      <PostEditor />
    </Suspense>
  )
}
