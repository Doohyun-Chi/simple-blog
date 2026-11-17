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

/** Mirrors `PostInput` in be/apispec.yaml. The slug is as readers see it, which for the allowed characters is the stored form. */
export interface PostInput {
  title: string
  slug: string
  content: string
}

/**
 * The longest title, in characters (Unicode code points). Mirrors `maxLength` of `PostInput.title` in be/apispec.yaml.
 * It is shown to the writer but not enforced here: the backend answers 400 for a longer title.
 */
export const POST_TITLE_MAX_LENGTH = 130

/** The longest slug. Mirrors `maxLength` of `PostInput.slug` in be/apispec.yaml; shown, not enforced, like the title's. */
export const POST_SLUG_MAX_LENGTH = 200

/** What a slug must not contain: the spec allows only English letters and `-`. */
const SLUG_FORBIDDEN = /[^A-Za-z-]/g

/** Turns typed text into slug characters: spaces become `-` and other forbidden characters are dropped. */
export function cleanSlug(slug: string): string {
  return slug.replace(/\s/g, '-').replace(SLUG_FORBIDDEN, '')
}

/** Whether a post can be sent: a title with a visible character and a slug made of allowed characters. */
export function isSavable(input: PostInput): boolean {
  return input.title.trim() !== '' && input.slug !== '' && cleanSlug(input.slug) === input.slug
}
