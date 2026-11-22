import type { ReactNode } from 'react'
import { cleanSlug, type PostType } from '../model/Post.ts'
import { googleLoginUrl } from '../model/identityApi.ts'
import { contentPath } from '../model/route.ts'
import type { RemoveFailure } from '../model/usePostDraft.ts'
import type { SaveFailure, SaveState } from '../model/useSavePost.ts'

const REMOVE_FAILURE_TEXT: Record<RemoveFailure, string> = {
  forbidden: '작성자만 글을 삭제할 수 있습니다.',
  failed: '삭제하지 못했습니다. 로그인 상태를 확인하고 잠시 후 다시 시도해 주세요.',
}

const SAVE_FAILURE_TEXT: Record<SaveFailure, string> = {
  invalid: '제목, Slug, 해시태그를 확인해 주세요.',
  duplicate: '이미 사용 중인 Slug입니다.',
  expired: '로그인이 만료되어 저장하지 못했습니다.',
  forbidden: '저장할 권한이 없습니다.',
  missing: '저장된 글을 찾을 수 없습니다. 다시 저장하면 새 글로 저장합니다.',
  failed: '저장하지 못했습니다. 잠시 후 다시 시도해 주세요.',
}

/** The line under a field. */
export function FieldHint({ note }: { note: ReactNode }) {
  return (
    <div className="post-editor-hint">
      <span className="post-editor-note" role="status">{note}</span>
    </div>
  )
}

function saveNote(state: SaveState, published: boolean, contentType: PostType): ReactNode {
  switch (state.status) {
    case 'idle':
      return null
    case 'saving':
      return '저장 중…'
    case 'saved':
      return <>{published ? '저장했습니다.' : '초안으로 저장했습니다.'} <a href={contentPath(contentType, state.slug)}>보기</a></>
    case 'failed':
      return SAVE_FAILURE_TEXT[state.failure]
  }
}

/** The save in progress or failed, else a warning while changes are unsaved, else the last save. */
function noteOf(state: SaveState, unsaved: boolean, published: boolean, contentType: PostType): ReactNode {
  const busyOrFailed = state.status === 'saving' || state.status === 'failed'
  return unsaved && !busyOrFailed ? '저장하지 않은 변경이 있습니다.' : saveNote(state, published, contentType)
}

interface TitleNoteProps {
  saveState: SaveState
  unsaved: boolean
  published: boolean
  contentType: PostType
  removeFailure: RemoveFailure | null
}

/** The line under the title: why the last removal failed, else how saving stands. */
export function TitleNote({ saveState, unsaved, published, contentType, removeFailure }: TitleNoteProps) {
  const note = removeFailure === null ? noteOf(saveState, unsaved, published, contentType) : REMOVE_FAILURE_TEXT[removeFailure]
  return <FieldHint note={note} />
}

/** The line under the slug: the characters a new slug may have, and that a slug the post already has can stay as it is. */
export function SlugNote({ slug, savedSlug }: { slug: string; savedSlug: string }) {
  const rule = '영문 대소문자, 숫자와 - 만 사용할 수 있습니다.'
  return <FieldHint note={slug === savedSlug && cleanSlug(slug) !== slug ? `기존 slug는 그대로 저장됩니다. 바꾸려면 ${rule}` : rule} />
}

/** Tells the writer the session ended and how to sign in again without leaving the editor, so nothing written is lost. */
export function SessionExpiredNote() {
  return (
    <FieldHint
      note={
        <>
          로그인이 만료되었습니다.
          {' '}
          <a href={googleLoginUrl()} target="_blank" rel="noreferrer">새 탭에서 다시 로그인</a>
          한 뒤 저장해 주세요. 작성 중인 내용은 이 탭에 그대로 남아 있습니다.
        </>
      }
    />
  )
}
