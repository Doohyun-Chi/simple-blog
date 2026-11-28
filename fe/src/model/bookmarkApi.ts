import { requestJson, ApiError, apiUrl } from './apiClient.ts'
import type { Bookmark, BookmarkArrangementItem, BookmarkList } from './Bookmark.ts'
import { csrfHeaders } from './csrfToken.ts'

const BOOKMARKS_PATH = '/public/api/v1/bookmarks'
const BOOKMARK_PATH = '/public/api/v1/bookmark'

function jsonRequest(method: string, body: unknown, signal?: AbortSignal): RequestInit {
  return { method, headers: { 'Content-Type': 'application/json', ...csrfHeaders() }, body: JSON.stringify(body), credentials: 'include', signal }
}

/** Calls `GET /public/api/v1/bookmarks` (operationId: listOfBookmarks): the signed-in user's bookmarks, the pinned first. */
export async function listOfBookmarks(limit?: number, signal?: AbortSignal): Promise<Bookmark[]> {
  const query = limit === undefined ? '' : `?${new URLSearchParams({ limit: String(limit) })}`
  const body = await requestJson<BookmarkList>(`${BOOKMARKS_PATH}${query}`, { signal, credentials: 'include' })
  return body.bookmarks
}

/** Calls `PUT /public/api/v1/bookmarks` (operationId: saveBookmarks): saves the names and orders at once. */
export async function saveBookmarks(bookmarks: BookmarkArrangementItem[], signal?: AbortSignal): Promise<Bookmark[]> {
  const body = await requestJson<BookmarkList>(BOOKMARKS_PATH, jsonRequest('PUT', { bookmarks }, signal))
  return body.bookmarks
}

/** Calls `POST /public/api/v1/bookmark` (operationId: createBookmark) with the slug as readers see it. */
export function createBookmark(slug: string, signal?: AbortSignal): Promise<Bookmark> {
  return requestJson<Bookmark>(BOOKMARK_PATH, jsonRequest('POST', { slug }, signal))
}

/** Calls `PUT /public/api/v1/bookmark/{id}` (operationId: renameBookmark). */
export function renameBookmark(id: number, name: string, signal?: AbortSignal): Promise<Bookmark> {
  return requestJson<Bookmark>(`${BOOKMARK_PATH}/${id}`, jsonRequest('PUT', { name }, signal))
}

/** Calls `DELETE /public/api/v1/bookmark/{id}` (operationId: deleteBookmark). The response has no body. */
export async function deleteBookmark(id: number, signal?: AbortSignal): Promise<void> {
  const response = await fetch(apiUrl(`${BOOKMARK_PATH}/${id}`), { method: 'DELETE', headers: csrfHeaders(), credentials: 'include', signal })
  if (!response.ok) {
    throw new ApiError(`DELETE ${BOOKMARK_PATH}/${id} failed with ${response.status}`, response.status)
  }
}
