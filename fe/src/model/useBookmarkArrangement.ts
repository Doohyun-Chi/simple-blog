import { useState } from 'react'
import { arrangementOf, type Bookmark, type BookmarkGroups, groupsOf, layoutOf, moved, named, pinned, unpinned, without } from './Bookmark.ts'
import { deleteBookmark, renameBookmark, saveBookmarks } from './bookmarkApi.ts'
import { announceBookmarkChange } from './useBookmarks.ts'

/** What went wrong last on the management page, for the component to word. */
export type ArrangementFailure = 'rename' | 'remove' | 'save'

/**
 * The management page's state. Pinning, unpinning and moving only change what is shown until `save`; a rename and a removal are sent at
 * once. `savedNames` holds the names the server has, to send a rename only when the name changed.
 */
export function useBookmarkArrangement(initial: Bookmark[]) {
  const [groups, updateGroups] = useState<BookmarkGroups>(() => groupsOf(initial))
  const [saved, updateSaved] = useState<BookmarkGroups>(() => groupsOf(initial))
  const [savedNames, updateSavedNames] = useState(() => new Map(initial.map((bookmark) => [bookmark.id, bookmark.name])))
  const [busy, updateBusy] = useState(false)
  const [failure, updateFailure] = useState<ArrangementFailure | null>(null)

  /** Runs a call to the server; a failure is kept for the page to show. */
  const attempt = async (what: ArrangementFailure, call: () => Promise<void>) => {
    updateBusy(true)
    updateFailure(null)
    try {
      await call()
    } catch (error: unknown) {
      console.error(error)
      updateFailure(what)
    } finally {
      updateBusy(false)
    }
  }

  const commitName = (id: number) => {
    const current = [...groups.pinned, ...groups.others].find((bookmark) => bookmark.id === id)
    if (current === undefined || current.name.trim() === '' || current.name === savedNames.get(id)) {
      return Promise.resolve()
    }
    return attempt('rename', async () => {
      const renamed = await renameBookmark(id, current.name)
      updateGroups((now) => named(now, id, renamed.name))
      updateSavedNames((now) => new Map(now).set(id, renamed.name))
      announceBookmarkChange()
    })
  }

  const remove = (id: number) => attempt('remove', async () => {
    await deleteBookmark(id)
    updateGroups((now) => without(now, id))
    updateSaved((now) => without(now, id))
    announceBookmarkChange()
  })

  const save = () => attempt('save', async () => {
    const result = groupsOf(await saveBookmarks(arrangementOf(groups)))
    updateGroups(result)
    updateSaved(result)
    announceBookmarkChange()
  })

  return {
    groups,
    busy,
    failure,
    changed: layoutOf(groups) !== layoutOf(saved),
    pin: (id: number) => updateGroups((now) => pinned(now, id)),
    unpin: (id: number) => updateGroups((now) => unpinned(now, id)),
    move: (id: number, targetId: number) => updateGroups((now) => moved(now, id, targetId)),
    rename: (id: number, name: string) => updateGroups((now) => named(now, id, name)),
    commitName,
    remove,
    save,
  }
}
