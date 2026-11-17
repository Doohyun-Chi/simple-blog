import { useRef, useState } from 'react'
import { ApiError } from './apiClient.ts'
import type { PostInput } from './Post.ts'
import { createPost, updatePost } from './postApi.ts'

/** Why a save failed, in the terms the writer can act on. */
export type SaveFailure = 'invalid' | 'duplicate' | 'forbidden' | 'missing' | 'failed'

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

/**
 * Saves the post being written. The first save creates a draft; every later save updates that same post, even
 * when the slug was changed in between. If the post has disappeared (404) the next save creates it again.
 */
export function useSavePost(): { state: SaveState; save: (input: PostInput) => Promise<void> } {
  const [state, updateState] = useState<SaveState>({ status: 'idle' })
  const savedSlug = useRef<string | null>(null)

  async function save(input: PostInput): Promise<void> {
    updateState({ status: 'saving' })
    try {
      await (savedSlug.current === null ? createPost(input) : updatePost(savedSlug.current, input))
      savedSlug.current = input.slug
      updateState({ status: 'saved', slug: input.slug })
    } catch (error: unknown) {
      const failure = failureOf(error)
      if (failure === 'missing') {
        savedSlug.current = null
      }
      console.error(error)
      updateState({ status: 'failed', failure })
    }
  }

  return { state, save }
}
