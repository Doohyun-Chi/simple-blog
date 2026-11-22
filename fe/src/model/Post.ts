/** Values of the `queryType` parameter of `GET /public/api/v1/posts` in be/apispec.yaml. */
export type PostQueryType = 'keyword' | 'tag'

export const DEFAULT_POST_QUERY_TYPE: PostQueryType = 'keyword'

/** Values of `PostBody.type` in be/apispec.yaml. Each is also the first path segment readers open it at: /post/{slug}, /page/{slug}. */
export const PostType = { POST: 'post', PAGE: 'page' } as const
export type PostType = (typeof PostType)[keyof typeof PostType]

/** Values of `PostBody.status` and `PostSummary.status` in be/apispec.yaml. */
export const PostStatus = { DRAFT: 'draft', PUBLISH: 'publish', PRIVATE: 'private' } as const
export type PostStatus = (typeof PostStatus)[keyof typeof PostStatus]

/** The statuses a writer may send in `PostInput.status`. */
export type SettablePostStatus = typeof PostStatus.DRAFT | typeof PostStatus.PUBLISH

/** Mirrors `PostSummary` in be/apispec.yaml. `slug` is the stored form; see readerSlug. */
export interface PostSummary {
  id: number
  title: string
  slug: string
  /** publish, or draft or private for the signed-in user's own posts. */
  status: PostStatus
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
  status: PostStatus
  type: PostType
  date: string
  modified: string
  /** Comma separated, without the # prefix; empty when there are none. See hashTagsOf. */
  hashTags: string
  /** Whether the visitor may revise and delete this post: signed in and its author. Decided by the backend. */
  canEdit: boolean
}

/** The hash tags of the comma separated stored text: each trimmed, empty ones skipped. */
export function hashTagsOf(hashTags: string): string[] {
  return hashTags.split(',').map((tag) => tag.trim()).filter((tag) => tag !== '')
}

/** The text a writer typed as hash tags (`#a, #b c`) in the stored form: tags without #, trimmed, joined by commas. */
export function hashTagsText(typed: string): string {
  return typed
    .split(',')
    .map((tag) => tag.trim().replace(/^#+\s*/, ''))
    .filter((tag) => tag !== '')
    .join(',')
}

/** The hash tags of a saved post as the writer types them: each with a # prefix, separated by a comma and a space. */
export function typedHashTags(hashTags: string): string {
  return hashTagsOf(hashTags).map((tag) => `#${tag}`).join(', ')
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

/** The tag for a status that is not public: 초안 for a draft, 비공개 for a private post. Nothing for a published one. */
export function statusLabelOf(status: PostStatus): string | null {
  switch (status) {
    case PostStatus.DRAFT:
      return '초안'
    case PostStatus.PRIVATE:
      return '비공개'
    case PostStatus.PUBLISH:
      return null
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
  /** Optional: publish makes the post public. Left out, a new post is a draft and an updated one keeps its status. */
  status?: SettablePostStatus
  /** Optional: comma separated, without the # prefix; an empty string removes the tags, left out keeps them. See hashTagsText. */
  hashTags?: string
  /** Optional: slugs of images the edit replaced; once the post is saved, those nothing shows any more are deleted. */
  replacedMedia?: string[]
}

/** The most slugs one save may send in `replacedMedia`. Mirrors its `maxItems` in be/apispec.yaml. */
export const REPLACED_MEDIA_MAX = 100

/**
 * The longest title, in characters (Unicode code points). Mirrors `maxLength` of `PostInput.title` in be/apispec.yaml.
 * It is shown to the writer but not enforced here: the backend answers 400 for a longer title.
 */
export const POST_TITLE_MAX_LENGTH = 130

/** The longest hash tag text, commas included. Mirrors `maxLength` of `PostInput.hashTags` in be/apispec.yaml; enforced by the backend. */
export const POST_HASH_TAGS_MAX_LENGTH = 200

/** The longest slug. Mirrors `maxLength` of `PostInput.slug` in be/apispec.yaml; shown, not enforced, like the title's. */
export const POST_SLUG_MAX_LENGTH = 200

/** What a slug must not contain: the spec allows only English letters, digits and `-`. */
const SLUG_FORBIDDEN = /[^A-Za-z0-9-]/g

/** Turns typed text into slug characters: spaces become `-` and other forbidden characters are dropped. */
export function cleanSlug(slug: string): string {
  return slug.replace(/\s/g, '-').replace(SLUG_FORBIDDEN, '')
}

/**
 * Whether a post can be sent: a title with a visible character and a slug made of allowed characters. A slug the post
 * already has (`savedSlug`) passes as it is, as the backend keeps it on update; rows migrated from WordPress often
 * have an underscore or Korean in it.
 */
export function isSavable(input: PostInput, savedSlug: string): boolean {
  return input.title.trim() !== '' && input.slug !== '' && (cleanSlug(input.slug) === input.slug || input.slug === savedSlug)
}

/** Whether the post being written differs from what was last saved (nothing saved yet counts as an empty post). */
export function isUnsaved(input: PostInput, saved: PostInput): boolean {
  return input.title !== saved.title || input.slug !== saved.slug || input.content.trim() !== saved.content.trim() ||
    (input.hashTags ?? '') !== (saved.hashTags ?? '')
}

/** The post before anything is written or saved. */
export const EMPTY_POST: PostInput = { title: '', slug: '', content: '', hashTags: '' }
