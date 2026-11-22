import { DEFAULT_POST_QUERY_TYPE, type PostQueryType, postQueryTypeOf, PostType } from './Post.ts'

export type Route =
  | { kind: 'home' }
  | { kind: 'content'; type: PostType; slug: string }
  | { kind: 'list' }
  | { kind: 'write' }
  | { kind: 'edit'; slug: string }
  | { kind: 'search'; queryType: PostQueryType; query: string }
  | { kind: 'notFound'; path: string }

/** The list of all posts, linked as `전체 목록` from the info area. */
export const LIST_PATH = '/posts'

/** The area for writing a new post, linked as `새로운 생각` from the info area. */
export const WRITE_PATH = '/write'

/** Matches /edit/{slug}: the area for revising an existing post, linked as `편집하기` from the post. */
const EDIT_PATH = /^\/edit\/([^/]+)\/?$/

/** Post search results: /search?query={query}&queryType={queryType}. The parameter names follow be/apispec.yaml. */
const SEARCH_PATH = /^\/search\/?$/

/** Matches /page/{slug} and /post/{slug}, with an optional trailing slash. A slug is a single path segment. */
const CONTENT_PATH = new RegExp(`^/(${Object.values(PostType).join('|')})/([^/]+)/?$`)
const TYPE_INDEX = 1
const SLUG_INDEX = 2

/** Resolves a URL path such as `/page/about`, with its query string such as `?query=kpi`, to the route it represents. */
export function routeOf(pathname: string, search: string = ''): Route {
  if (pathname === '' || pathname === '/') {
    return { kind: 'home' }
  }
  if (pathname.replace(/\/$/, '') === LIST_PATH) {
    return { kind: 'list' }
  }
  if (pathname.replace(/\/$/, '') === WRITE_PATH) {
    return { kind: 'write' }
  }
  if (SEARCH_PATH.test(pathname)) {
    return searchRouteOf(new URLSearchParams(search))
  }
  const edit = EDIT_PATH.exec(pathname)
  if (edit) {
    const editSlug = decodedSlug(edit[1])
    return editSlug === null ? { kind: 'notFound', path: pathname } : { kind: 'edit', slug: editSlug }
  }
  const match = CONTENT_PATH.exec(pathname)
  const slug = match ? decodedSlug(match[SLUG_INDEX]) : null
  if (!match || slug === null) {
    return { kind: 'notFound', path: pathname }
  }
  return { kind: 'content', type: match[TYPE_INDEX] as PostType, slug }
}

/** The slug of the page being shown, or null when the route is not a page (home, a post, or not found). */
export function currentPageSlug(route: Route): string | null {
  return route.kind === 'content' && route.type === PostType.PAGE ? route.slug : null
}

/** Builds the URL path for a piece of content, encoding the slug so non-ASCII slugs stay valid. */
export function contentPath(type: PostType, slug: string): string {
  return `/${type}/${encodeURIComponent(slug)}`
}

/** Builds the URL path for revising a post, encoding the slug like contentPath. */
export function editPath(slug: string): string {
  return `/edit/${encodeURIComponent(slug)}`
}

/** Builds the URL path for a post search. The default query type is left out to keep the URL short. */
export function searchPath(query: string, queryType: PostQueryType = DEFAULT_POST_QUERY_TYPE): string {
  const params = new URLSearchParams({ query })
  if (queryType !== DEFAULT_POST_QUERY_TYPE) {
    params.set('queryType', queryType)
  }
  return `/search?${params}`
}

/** The query being searched, or an empty string when the route is not a search. */
export function currentQuery(route: Route): string {
  return route.kind === 'search' ? route.query : ''
}

/** A search with a blank query has nothing to show, so it falls back to home, as the spec forbids sending it. */
function searchRouteOf(params: URLSearchParams): Route {
  const query = params.get('query') ?? ''
  if (query.trim() === '') {
    return { kind: 'home' }
  }
  return { kind: 'search', queryType: postQueryTypeOf(params.get('queryType')), query }
}

/** Undoes the URL encoding from contentPath, so the slug matches the stored value. A malformed escape yields null. */
function decodedSlug(rawSlug: string): string | null {
  try {
    return decodeURIComponent(rawSlug)
  } catch {
    return null
  }
}
