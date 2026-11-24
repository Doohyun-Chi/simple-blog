import { ApiError, apiUrl, requestJson } from './apiClient.ts'
import type { Book, BookInput, BookList, BookPreview, Reading } from './Book.ts'
import { csrfHeaders } from './csrfToken.ts'

const BOOKS_PATH = '/public/api/v1/books'
const BOOK_PATH = '/public/api/v1/book'
const PREVIEW_PATH = '/public/api/v1/book-preview'

/** Calls `GET /public/api/v1/books` (operationId: listOfBooks): newest first, of the year when one is given. */
export function listOfBooks(year: number | null, signal?: AbortSignal): Promise<BookList> {
  const query = year === null ? '' : `?${new URLSearchParams({ year: String(year) })}`
  return requestJson<BookList>(`${BOOKS_PATH}${query}`, { signal })
}

/** Calls `GET /public/api/v1/book-preview` (operationId: previewBook): what the store page says about the book. Needs the session. */
export function previewBook(url: string, signal?: AbortSignal): Promise<BookPreview> {
  return requestJson<BookPreview>(`${PREVIEW_PATH}?${new URLSearchParams({ url })}`, { signal, credentials: 'include' })
}

/** Calls `POST /public/api/v1/book` (operationId: createBook): registers an unread book, dated today. */
export function createBook(input: BookInput, signal?: AbortSignal): Promise<Book> {
  return requestJson<Book>(BOOK_PATH, jsonRequest('POST', input, signal))
}

/** Calls `PUT /public/api/v1/book/{id}` (operationId: updateBook): replaces the title, author, description and the two URLs; the rest stays. */
export function updateBook(id: number, input: BookInput, signal?: AbortSignal): Promise<Book> {
  return requestJson<Book>(`${BOOK_PATH}/${id}`, jsonRequest('PUT', input, signal))
}

/** Calls `PUT /public/api/v1/book/{id}/reading` (operationId: checkInBook): both dates are replaced as sent. */
export function checkInBook(id: number, reading: Reading, signal?: AbortSignal): Promise<Book> {
  return requestJson<Book>(`${BOOK_PATH}/${id}/reading`, jsonRequest('PUT', reading, signal))
}

/** Calls `PUT /public/api/v1/book/{id}/insight` (operationId: saveBookInsight): an empty text clears it. */
export function saveBookInsight(id: number, insight: string, signal?: AbortSignal): Promise<Book> {
  return requestJson<Book>(`${BOOK_PATH}/${id}/insight`, jsonRequest('PUT', { insight }, signal))
}

/** Calls `PUT /public/api/v1/book/{id}/review` (operationId: linkBookReview): the post need not exist yet. */
export function linkBookReview(id: number, trackbackSlug: string | null, signal?: AbortSignal): Promise<Book> {
  return requestJson<Book>(`${BOOK_PATH}/${id}/review`, jsonRequest('PUT', { trackbackSlug }, signal))
}

/** Calls `DELETE /public/api/v1/book/{id}` (operationId: deleteBook): the book leaves the shelf for good. The response has no body. */
export async function deleteBook(id: number, signal?: AbortSignal): Promise<void> {
  const response = await fetch(apiUrl(`${BOOK_PATH}/${id}`), { method: 'DELETE', headers: csrfHeaders(), credentials: 'include', signal })
  if (!response.ok) {
    throw new ApiError(`DELETE ${BOOK_PATH}/${id} failed with ${response.status}`, response.status)
  }
}

/** A write rides on the session cookie, and the backend wants the CSRF token it published echoed in a header. */
function jsonRequest(method: string, body: unknown, signal?: AbortSignal): RequestInit {
  const headers = { 'Content-Type': 'application/json', ...csrfHeaders() }
  return { method, headers, body: JSON.stringify(body), credentials: 'include', signal }
}
