import type { PostBody } from './Post.ts'
import { postBody } from './postApi.ts'
import { type Remote, useRemote } from './useRemote.ts'

/** Loads the body of the post or page with this slug, as readers see it. */
export function usePostBody(slug: string): Remote<PostBody> {
  return useRemote(slug, (signal) => postBody(slug, signal))
}
