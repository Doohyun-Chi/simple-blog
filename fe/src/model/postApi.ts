import { requestJson } from './apiClient.ts'
import type { PostBody, PostList, PostQueryType, PostSummary } from './Post.ts'

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
