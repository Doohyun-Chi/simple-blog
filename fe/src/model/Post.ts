/** Values of the `queryType` parameter of `GET /public/api/v1/posts` in be/apispec.yaml. */
export type PostQueryType = 'keyword' | 'tag'

export const DEFAULT_POST_QUERY_TYPE: PostQueryType = 'keyword'

/** Mirrors `PostSummary` in be/apispec.yaml. `slug` is the stored form; see readerSlug. */
export interface PostSummary {
  id: number
  title: string
  slug: string
  date: string
}

/** Mirrors `PostList` in be/apispec.yaml. */
export interface PostList {
  posts: PostSummary[]
}

/** Mirrors `PostBody` in be/apispec.yaml. `content` is stored WordPress markup, not rendered HTML. */
export interface PostBody {
  title: string
  content: string
  status: string
  date: string
  modified: string
}

/** Narrows a raw query string value to a known query type, falling back to the spec default. */
export function postQueryTypeOf(value: string | null): PostQueryType {
  return value === 'tag' || value === 'keyword' ? value : DEFAULT_POST_QUERY_TYPE
}

/**
 * The slug readers see. WordPress stores non-ASCII slugs lowercase percent-encoded, so the spec says to decode once.
 * A malformed escape is kept as stored.
 */
export function readerSlug(storedSlug: string): string {
  try {
    return decodeURIComponent(storedSlug)
  } catch {
    return storedSlug
  }
}

/** The calendar day of a local `yyyy-MM-ddTHH:mm:ss` time from the spec, as `yyyy-MM-dd`. */
export function dayOf(localDateTime: string): string {
  return localDateTime.slice(0, 'yyyy-MM-dd'.length)
}

/**
 * Whether the content comes from the classic editor. Classic content has no block comments and separates paragraphs
 * with blank lines instead of <p> tags, which WordPress itself turns into paragraphs only when rendering (wpautop).
 */
export function isClassicContent(content: string): boolean {
  return !content.includes('<!-- wp:')
}
