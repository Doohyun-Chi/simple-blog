import { lazy, Suspense } from 'react'
import { StatusMessage } from '../component/StatusMessage.tsx'
import type { PostInput } from '../model/Post.ts'
import { useSavePost } from '../model/useSavePost.ts'

/** The MDX editor is large, so it loads only when the writing area is opened and reading pages stay light. */
const PostEditor = lazy(() => import('../component/PostEditor.tsx').then((module) => ({ default: module.PostEditor })))

interface PostWriterProps {
  /** The post to revise: the editor starts with it and saves update it. Without it, a new post is written. */
  initial?: PostInput
}

/** The content area for /write and /edit/{slug}: the editor, saving through the backend. */
export function PostWriter({ initial }: PostWriterProps) {
  const { state, save, remove } = useSavePost(initial?.slug ?? null)
  return (
    <Suspense fallback={<StatusMessage>편집기를 불러오는 중…</StatusMessage>}>
      <PostEditor saveState={state} onSave={save} onRemove={remove} initial={initial} />
    </Suspense>
  )
}
