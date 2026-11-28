import { useState } from 'react'
import { createBookmark, deleteBookmark } from './bookmarkApi.ts'
import { announceBookmarkChange } from './useBookmarks.ts'

/** The star of a post: whether the signed-in user has bookmarked it (`id` is the bookmark's), and the switch that adds or removes it. */
export function useBookmarkToggle(slug: string, initialId: number | null) {
  const [id, updateId] = useState<number | null>(initialId)
  const [busy, updateBusy] = useState(false)
  const [failed, updateFailed] = useState(false)
  const toggle = async () => {
    updateBusy(true)
    updateFailed(false)
    try {
      if (id === null) {
        updateId((await createBookmark(slug)).id)
      } else {
        await deleteBookmark(id)
        updateId(null)
      }
      announceBookmarkChange()
    } catch (error: unknown) {
      console.error(error)
      updateFailed(true)
    } finally {
      updateBusy(false)
    }
  }
  return { bookmarked: id !== null, busy, failed, toggle }
}
