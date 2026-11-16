import type { PostQueryType, PostSummary } from './Post.ts'
import { listOfPosts } from './postApi.ts'
import { type Remote, useRemote } from './useRemote.ts'

/** How many posts the info area shows. The spec allows 1 to 100. */
const RECENT_POST_LIMIT = 10

/** The most posts the list view shows (fe/DESIGN.md). Paging with PREV and NEXT needs an offset in the spec first. */
const LIST_POST_LIMIT = 20

/** Loads the newest published posts matching the query. The query must not be blank; see the spec. */
export function useListOfPosts(queryType: PostQueryType, query: string): Remote<PostSummary[]> {
  return useRemote(JSON.stringify([queryType, query]), (signal) =>
    listOfPosts({ queryType, query, limit: LIST_POST_LIMIT }, signal),
  )
}

/** Loads the newest published posts for the list view, using the same endpoint as search but with no query. */
export function useListOfAllPosts(): Remote<PostSummary[]> {
  return useRemote('all', (signal) => listOfPosts({ limit: LIST_POST_LIMIT }, signal))
}

/** Loads the newest published posts for the info area and the home page. */
export function useListOfRecentPosts(): Remote<PostSummary[]> {
  return useRemote('recent', (signal) => listOfPosts({ limit: RECENT_POST_LIMIT }, signal))
}
