import { useEffect, useState } from 'react'
import {
  cleanSlug,
  EMPTY_POST,
  hashTagsText,
  isSavable,
  isUnsaved,
  type PostInput,
  PostStatus,
  type SettablePostStatus,
  typedHashTags,
} from './Post.ts'
import { LIST_PATH } from './route.ts'
import { useLeaveGuard } from './useLeaveGuard.ts'
import type { RemoveResult, SaveState } from './useSavePost.ts'
import { useSessionKeepAlive } from './useSessionKeepAlive.ts'

/** Why a removal did not delete the post. */
export type RemoveFailure = Exclude<RemoveResult, 'removed'>

/** The fields as typed. The slug keeps only the characters the spec allows; every edit keeps the session alive. */
function useDraftFields(initial: PostInput, touch: () => void) {
  const [title, updateTitle] = useState(initial.title)
  const [slug, updateSlug] = useState(initial.slug)
  const [content, updateContent] = useState(initial.content)
  const [typedTags, updateTypedTags] = useState(typedHashTags(initial.hashTags ?? ''))
  function touched(update: (value: string) => void): (value: string) => void {
    return (value) => {
      update(value)
      touch()
    }
  }
  return {
    title, slug, content, typedTags,
    editTitle: touched(updateTitle),
    editSlug: touched((value) => updateSlug(cleanSlug(value))),
    editContent: touched(updateContent),
    editTags: touched(updateTypedTags),
  }
}

/** What was last saved, whether the post is published, and whether it exists on the backend yet. */
function useSavedPost(initial: PostInput) {
  const [saved, updateSaved] = useState<PostInput>(initial)
  const [published, updatePublished] = useState(initial.status === PostStatus.PUBLISH)
  const [exists, updateExists] = useState(initial !== EMPTY_POST)
  function markSaved(input: PostInput, publishedNow: boolean) {
    updateSaved(input)
    updateExists(true)
    if (publishedNow) {
      updatePublished(true)
    }
  }
  return { saved, published, exists, markSaved }
}

/** Deletes the post once `confirmed` says yes; once it is gone the page leaves for the list of all posts. */
function useRemoval(onRemove: () => Promise<RemoveResult>) {
  const [removed, updateRemoved] = useState(false)
  const [removeFailure, updateRemoveFailure] = useState<RemoveFailure | null>(null)
  useEffect(() => {
    if (removed) {
      window.location.assign(LIST_PATH)
    }
  }, [removed])
  async function remove(confirmed: () => boolean) {
    if (!confirmed()) {
      return
    }
    updateRemoveFailure(null)
    const result = await onRemove()
    if (result === 'removed') {
      updateRemoved(true)
    } else {
      updateRemoveFailure(result)
    }
  }
  return { removed, removeFailure, remove }
}

interface PostDraftOptions {
  initial: PostInput
  saveState: SaveState
  onSave: (input: PostInput) => Promise<boolean>
  onRemove: () => Promise<RemoveResult>
}

/** Whether the session has ended: the keep-alive found nobody signed in, or the last save was refused for it. */
function sessionExpiredOf(keepAliveExpired: boolean, saveState: SaveState): boolean {
  return keepAliveExpired || (saveState.status === 'failed' && saveState.failure === 'expired')
}

/**
 * The post being written in the editor: its fields, what the buttons may do, and the actions behind them. Leaving the
 * page with unsaved changes asks first, unless the post was deleted. 공개 saves with status publish; a plain save sends
 * no status, so an updated post keeps its own.
 */
export function usePostDraft({ initial, saveState, onSave, onRemove }: PostDraftOptions) {
  const keepAlive = useSessionKeepAlive()
  const fields = useDraftFields(initial, keepAlive.touch)
  const record = useSavedPost(initial)
  const removal = useRemoval(onRemove)
  const input = { title: fields.title, slug: fields.slug, content: fields.content, hashTags: hashTagsText(fields.typedTags) }
  const saving = saveState.status === 'saving'
  const unsaved = isUnsaved(input, record.saved)
  useLeaveGuard(unsaved && !removal.removed)
  async function saveWith(status?: SettablePostStatus) {
    if (await onSave(status === undefined ? input : { ...input, status })) {
      record.markSaved(input, status === PostStatus.PUBLISH)
    }
  }
  return {
    ...fields,
    savedSlug: record.saved.slug,
    published: record.published,
    unsaved,
    canSave: isSavable(input, record.saved.slug) && !saving,
    canRemove: record.exists && !removal.removed && !saving,
    sessionExpired: sessionExpiredOf(keepAlive.expired, saveState),
    removeFailure: removal.removeFailure,
    save: () => saveWith(),
    publish: () => saveWith(PostStatus.PUBLISH),
    remove: removal.remove,
  }
}
