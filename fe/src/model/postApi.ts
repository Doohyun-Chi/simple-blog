import { requestJson } from './apiClient.ts'
import type { PostBody, PostInput, PostList, PostQueryType, PostSummary } from './Post.ts'

const POSTS_PATH = '/public/api/v1/posts'
const POST_PATH = '/public/api/v1/post'

/** Parameters of `GET /public/api/v1/posts`. Without a query every published post matches. */
export interface PostListRequest {
  queryType?: PostQueryType
  /** Must not be blank; leave it out to match every post. */
  query?: string
  limit?: number
}

/** Calls `GET /public/api/v1/posts` (operationId: listOfPosts). Only the given parameters are sent. */
export async function listOfPosts(request: PostListRequest, signal?: AbortSignal): Promise<PostSummary[]> {
  const params = new URLSearchParams()
  for (const [name, value] of Object.entries(request)) {
    params.set(name, String(value))
  }
  const body = await requestJson<PostList>(`${POSTS_PATH}?${params}`, { signal })
  return body.posts
}

/** Calls `GET /public/api/v1/post/{slug}` (operationId: postBody) with the slug as readers see it. */
export function postBody(slug: string, signal?: AbortSignal): Promise<PostBody> {
  return requestJson<PostBody>(`${POST_PATH}/${encodeURIComponent(slug)}`, { signal })
}

/** Calls `POST /public/api/v1/post` (operationId: createPost). The new post is a draft. */
export function createPost(input: PostInput, signal?: AbortSignal): Promise<PostBody> {
  return requestJson<PostBody>(POST_PATH, jsonRequest('POST', input, signal))
}

/** Calls `PUT /public/api/v1/post/{slug}` (operationId: updatePost). A different slug in the input renames the post. */
export function updatePost(slug: string, input: PostInput, signal?: AbortSignal): Promise<PostBody> {
  return requestJson<PostBody>(`${POST_PATH}/${encodeURIComponent(slug)}`, jsonRequest('PUT', input, signal))
}

function jsonRequest(method: string, body: PostInput, signal?: AbortSignal): RequestInit {
  return { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body), signal }
}
