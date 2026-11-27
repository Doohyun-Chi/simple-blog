import type { PostList, PostListView, PostQueryType, PostSummary } from './Post.ts'
import { listOfPosts, pageOfPosts } from './postApi.ts'
import { type Remote, useRemote } from './useRemote.ts'

/** How many posts the info area shows. The spec allows 1 to 100. */
const RECENT_POST_LIMIT = 7

/** How many posts a page of the list view holds (fe/DESIGN.md). */
export const LIST_POST_LIMIT = 20

/** Which page of the list view to load: every post when there is no query, else the matches. */
export interface PostPageRequest {
  queryType?: PostQueryType
  query?: string
  offset: number
  view: PostListView
}

/** Loads one page of the list view, newest first. A query must not be blank; see the spec. */
export function useListOfPostPage(request: PostPageRequest): Remote<PostList> {
  return useRemote(JSON.stringify(request), (signal) => pageOfPosts({ ...request, limit: LIST_POST_LIMIT }, signal))
}

/** Loads the newest published posts for the info area and the home page. */
export function useListOfRecentPosts(): Remote<PostSummary[]> {
  return useRemote('recent', (signal) => listOfPosts({ limit: RECENT_POST_LIMIT }, signal))
}
