import { useRef, useState } from 'react'
import { ApiError } from './apiClient.ts'
import type { PostInput } from './Post.ts'
import { createPost, deletePost, updatePost } from './postApi.ts'

/** Why a save failed, in the terms the writer can act on. */
export type SaveFailure = 'invalid' | 'duplicate' | 'expired' | 'forbidden' | 'missing' | 'failed'

/** How a removal ended: the post is gone, only its author may delete it, or it failed for another reason. */
export type RemoveResult = 'removed' | 'forbidden' | 'failed'

export type SaveState =
  | { status: 'idle' }
  | { status: 'saving' }
  | { status: 'saved'; slug: string }
  | { status: 'failed'; failure: SaveFailure }

function failureOf(error: unknown): SaveFailure {
  if (!(error instanceof ApiError)) {
    return 'failed'
  }
  switch (error.status) {
    case 400:
      return 'invalid'
    case 401:
      return 'expired'
    case 403:
      return 'forbidden'
    case 404:
      return 'missing'
    case 409:
      return 'duplicate'
    default:
      return 'failed'
  }
}

function removeFailureOf(error: unknown): RemoveResult {
  if (error instanceof ApiError && error.status === 404) {
    return 'removed'
  }
  return error instanceof ApiError && error.status === 403 ? 'forbidden' : 'failed'
}

/**
 * Saves the post being written. The first save creates a draft; every later save updates that same post, even
 * when the slug was changed in between. Given the slug of an existing post, even the first save updates it. If the post has disappeared (404) the next save creates it again. A save
 * answers whether it succeeded. A removal deletes the post saved so far, and answers how that ended.
 */
export function useSavePost(existingSlug: string | null = null): {
  state: SaveState
  save: (input: PostInput) => Promise<boolean>
  remove: () => Promise<RemoveResult>
} {
  const [state, updateState] = useState<SaveState>({ status: 'idle' })
  const savedSlug = useRef<string | null>(existingSlug)

  async function save(input: PostInput): Promise<boolean> {
    updateState({ status: 'saving' })
    try {
      await (savedSlug.current === null ? createPost(input) : updatePost(savedSlug.current, input))
      savedSlug.current = input.slug
      updateState({ status: 'saved', slug: input.slug })
      return true
    } catch (error: unknown) {
      const failure = failureOf(error)
      if (failure === 'missing') {
        savedSlug.current = null
      }
      console.error(error)
      updateState({ status: 'failed', failure })
      return false
    }
  }

  /** Deletes the post saved so far. A post that is already gone (404) counts as deleted; nothing saved counts as well. */
  async function remove(): Promise<RemoveResult> {
    if (savedSlug.current === null) {
      return 'removed'
    }
    try {
      await deletePost(savedSlug.current)
      return 'removed'
    } catch (error: unknown) {
      console.error(error)
      return removeFailureOf(error)
    }
  }

  return { state, save, remove }
}
