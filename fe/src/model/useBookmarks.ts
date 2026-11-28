import { useEffect, useState } from 'react'
import { type Bookmark, RECENT_BOOKMARK_LIMIT } from './Bookmark.ts'
import { listOfBookmarks } from './bookmarkApi.ts'
import { type Remote, useRemote } from './useRemote.ts'

/** Sent on the window when a bookmark is added or removed, so what lists the bookmarks loads them again. */
const CHANGED_EVENT = 'simple-blog:bookmarks-changed'

/** Tells the lists of bookmarks on this page that the bookmarks changed. */
export function announceBookmarkChange() {
  window.dispatchEvent(new Event(CHANGED_EVENT))
}

/** A number that counts the changes announced since the page opened. */
function useBookmarkRevision(): number {
  const [revision, updateRevision] = useState(0)
  useEffect(() => {
    const change = () => updateRevision((current) => current + 1)
    window.addEventListener(CHANGED_EVENT, change)
    return () => window.removeEventListener(CHANGED_EVENT, change)
  }, [])
  return revision
}

/** Loads the newest bookmarks for the info area; nothing is asked of the server for a visitor who is not signed in. */
export function useListOfRecentBookmarks(signedIn: boolean): Remote<Bookmark[]> {
  const revision = useBookmarkRevision()
  const load = (signal: AbortSignal) => (signedIn ? listOfBookmarks(RECENT_BOOKMARK_LIMIT, signal) : Promise.resolve([]))
  return useRemote(`recent-bookmarks:${signedIn}:${revision}`, load)
}
