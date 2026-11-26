import { useState } from 'react'
import { changeFailureText, type Media } from './Media.ts'
import { renameMedia } from './mediaApi.ts'
import { trimmedMediaSlug } from './mediaSlug.ts'

export interface ImageSlugRename {
  typed: string
  failure: string | null
  busy: boolean
  /** Whether the typed slug can be sent: it is usable and not the slug the image has. */
  canRename: boolean
  edit: (text: string) => void
  rename: () => Promise<void>
}

/** The state of renaming an image: the slug being typed, and the call that renames it. `onRenamed` hears the renamed image. */
export function useImageSlugRename(slug: string, onRenamed: (media: Media) => void): ImageSlugRename {
  const [typed, updateTyped] = useState(slug)
  const [failure, updateFailure] = useState<string | null>(null)
  const [busy, updateBusy] = useState(false)
  const next = trimmedMediaSlug(typed)

  async function rename() {
    updateBusy(true)
    try {
      onRenamed(await renameMedia(slug, next))
    } catch (error: unknown) {
      console.error(error)
      updateFailure(changeFailureText(error))
      updateBusy(false)
    }
  }

  return { typed, failure, busy, canRename: next !== '' && next !== slug && !busy, edit: updateTyped, rename }
}
