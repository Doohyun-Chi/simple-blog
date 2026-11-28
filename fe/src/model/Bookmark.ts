/** Mirrors `Bookmark` in be/apispec.yaml. `slug` is the stored form; see readerSlug. `order` 0 is not pinned, above 0 is pinned. */
export interface Bookmark {
  id: number
  postId: number
  slug: string
  name: string
  order: number
  createdDate: string
  modifiedDate: string | null
}

/** Mirrors `BookmarkList` in be/apispec.yaml. */
export interface BookmarkList {
  bookmarks: Bookmark[]
}

/** One entry of `BookmarkArrangement` in be/apispec.yaml: what `PUT /public/api/v1/bookmarks` saves for a bookmark. */
export interface BookmarkArrangementItem {
  id: number
  name: string
  order: number
}

/** The most bookmarks the info area lists (fe/DESIGN.md). */
export const RECENT_BOOKMARK_LIMIT = 7

/** The bookmarks of the management page in two groups, each in the order it is shown top down. */
export interface BookmarkGroups {
  pinned: Bookmark[]
  others: Bookmark[]
}

/** Splits the list the server answers (pinned first, by the larger order) into the pinned and the other bookmarks. */
export function groupsOf(bookmarks: Bookmark[]): BookmarkGroups {
  return { pinned: bookmarks.filter((bookmark) => bookmark.order > 0), others: bookmarks.filter((bookmark) => bookmark.order <= 0) }
}

/** The bookmark moves to the top of the pinned ones. */
export function pinned(groups: BookmarkGroups, id: number): BookmarkGroups {
  const found = groups.others.find((bookmark) => bookmark.id === id)
  return found === undefined ? groups : { pinned: [found, ...groups.pinned], others: groups.others.filter((bookmark) => bookmark.id !== id) }
}

/** The bookmark moves to the top of the other ones. */
export function unpinned(groups: BookmarkGroups, id: number): BookmarkGroups {
  const found = groups.pinned.find((bookmark) => bookmark.id === id)
  return found === undefined ? groups : { pinned: groups.pinned.filter((bookmark) => bookmark.id !== id), others: [found, ...groups.others] }
}

/** The pinned bookmark `id` takes the place of the pinned bookmark `targetId`. */
export function moved(groups: BookmarkGroups, id: number, targetId: number): BookmarkGroups {
  const from = groups.pinned.findIndex((bookmark) => bookmark.id === id)
  const to = groups.pinned.findIndex((bookmark) => bookmark.id === targetId)
  if (from < 0 || to < 0 || from === to) {
    return groups
  }
  const list = [...groups.pinned]
  list.splice(to, 0, ...list.splice(from, 1))
  return { ...groups, pinned: list }
}

/** The bookmark with the new name, wherever it is. */
export function named(groups: BookmarkGroups, id: number, name: string): BookmarkGroups {
  const rename = (bookmark: Bookmark) => (bookmark.id === id ? { ...bookmark, name } : bookmark)
  return { pinned: groups.pinned.map(rename), others: groups.others.map(rename) }
}

/** Without the bookmark. */
export function without(groups: BookmarkGroups, id: number): BookmarkGroups {
  const keep = (bookmark: Bookmark) => bookmark.id !== id
  return { pinned: groups.pinned.filter(keep), others: groups.others.filter(keep) }
}

/** What to save: the pinned bookmarks take the orders n, n-1, ... 1 top down, so the top one comes first; the others take 0. */
export function arrangementOf(groups: BookmarkGroups): BookmarkArrangementItem[] {
  const pins = groups.pinned.map((bookmark, index) => ({ id: bookmark.id, name: bookmark.name, order: groups.pinned.length - index }))
  return [...pins, ...groups.others.map((bookmark) => ({ id: bookmark.id, name: bookmark.name, order: 0 }))]
}

/** Which bookmarks are pinned, and in what order: what saving the arrangement would change, so two arrangements are the same when it is. */
export function layoutOf(groups: BookmarkGroups): string {
  return JSON.stringify(arrangementOf(groups).map((item) => [item.id, item.order]))
}
