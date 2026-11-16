import type { PostQueryType, PostSummary } from './Post.ts'
import { listOfPosts } from './postApi.ts'
import { type Remote, useRemote } from './useRemote.ts'

/** How many posts the home page shows. The spec allows 1 to 100. */
const RECENT_POST_LIMIT = 10

/** Loads the published posts matching the query. The query must not be blank; see the spec. */
export function useListOfPosts(queryType: PostQueryType, query: string): Remote<PostSummary[]> {
  return useRemote(JSON.stringify([queryType, query]), (signal) => listOfPosts({ queryType, query }, signal))
}

/** Loads the newest published posts, using the same endpoint as search but with no query. */
export function useListOfRecentPosts(): Remote<PostSummary[]> {
  return useRemote('recent', (signal) => listOfPosts({ limit: RECENT_POST_LIMIT }, signal))
}
