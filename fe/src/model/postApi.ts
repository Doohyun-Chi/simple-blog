import { ApiError, apiUrl, requestJson } from './apiClient.ts'
import { csrfHeaders } from './csrfToken.ts'
import type { PostBody, PostInput, PostList, PostListView, PostQueryType, PostSummary } from './Post.ts'

const POSTS_PATH = '/public/api/v1/posts'
const POST_PATH = '/public/api/v1/post'

/** Parameters of `GET /public/api/v1/posts`. Without a query every published post matches. */
export interface PostListRequest {
  queryType?: PostQueryType
  /** Must not be blank; leave it out to match every post. */
  query?: string
  limit?: number
  /** How many of the newest matches to skip; 0 when left out. */
  offset?: number
  /** `card` adds a thumbnail and an excerpt to every post, which costs the server more. */
  view?: PostListView
}

/**
 * Calls `GET /public/api/v1/posts` (operationId: listOfPosts). Only the given parameters are sent. The session cookie goes
 * along, as the backend adds the signed-in user's own unpublished posts to the list.
 */
export async function listOfPosts(request: PostListRequest, signal?: AbortSignal): Promise<PostSummary[]> {
  return (await pageOfPosts(request, signal)).posts
}

/** The same call as {@link listOfPosts}, with whether a post follows the list. */
export async function pageOfPosts(request: PostListRequest, signal?: AbortSignal): Promise<PostList> {
  const params = new URLSearchParams()
  for (const [name, value] of Object.entries(request)) {
    params.set(name, String(value))
  }
  return requestJson<PostList>(`${POSTS_PATH}?${params}`, { signal, credentials: 'include' })
}

/**
 * Calls `GET /public/api/v1/post/{slug}` (operationId: postBody) with the slug as readers see it. The session cookie goes
 * along, as the backend tells from it whether the signed-in user may edit the post (`canEdit`).
 */
export function postBody(slug: string, signal?: AbortSignal): Promise<PostBody> {
  return requestJson<PostBody>(`${POST_PATH}/${encodeURIComponent(slug)}`, { signal, credentials: 'include' })
}

/** Calls `POST /public/api/v1/post` (operationId: createPost). The new post is a draft. */
export function createPost(input: PostInput, signal?: AbortSignal): Promise<PostBody> {
  return requestJson<PostBody>(POST_PATH, jsonRequest('POST', input, signal))
}

/** Calls `PUT /public/api/v1/post/{slug}` (operationId: updatePost). A different slug in the input renames the post. */
export function updatePost(slug: string, input: PostInput, signal?: AbortSignal): Promise<PostBody> {
  return requestJson<PostBody>(`${POST_PATH}/${encodeURIComponent(slug)}`, jsonRequest('PUT', input, signal))
}

/** Calls `DELETE /public/api/v1/post/{slug}` (operationId: deletePost). The response has no body. */
export async function deletePost(slug: string, signal?: AbortSignal): Promise<void> {
  const headers = csrfHeaders()
  const response = await fetch(apiUrl(`${POST_PATH}/${encodeURIComponent(slug)}`), {
    method: 'DELETE',
    headers,
    credentials: 'include',
    signal,
  })
  if (!response.ok) {
    throw new ApiError(`DELETE ${POST_PATH} failed with ${response.status}`, response.status)
  }
}

/** A write rides on the session cookie, and the backend wants the CSRF token it published echoed in a header. */
function jsonRequest(method: string, body: PostInput, signal?: AbortSignal): RequestInit {
  const headers = { 'Content-Type': 'application/json', ...csrfHeaders() }
  return { method, headers, body: JSON.stringify(body), credentials: 'include', signal }
}
