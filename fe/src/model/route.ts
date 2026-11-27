import { DEFAULT_POST_QUERY_TYPE, type PostQueryType, postQueryTypeOf, type PostStarter, PostType } from './Post.ts'

export type Route =
  | { kind: 'home' }
  | { kind: 'content'; type: PostType; slug: string }
  | { kind: 'list' }
  | { kind: 'write'; starter: PostStarter | null }
  | { kind: 'books' }
  | { kind: 'user' }
  | { kind: 'edit'; slug: string }
  | { kind: 'search'; queryType: PostQueryType; query: string }
  | { kind: 'notFound'; path: string }

/** The list of all posts, linked as `전체 목록` from the info area. */
export const LIST_PATH = '/posts'

/** The area for writing a new post, linked as `새로운 생각` from the info area. */
export const WRITE_PATH = '/write'

/** The bookshelf, linked as `서재` from the navigation. */
export const BOOKS_PATH = '/books'

/** The personalization page of the signed-in user, linked from the email in the navigation. */
export const USER_PATH = '/user'

/** Matches /edit/{slug}: the area for revising an existing post, linked as `편집하기` from the post. */
const EDIT_PATH = /^\/edit\/([^/]+)\/?$/

/** Post search results: /search?query={query}&queryType={queryType}. The parameter names follow be/apispec.yaml. */
const SEARCH_PATH = /^\/search\/?$/

/** Matches /page/{slug} and /post/{slug}, with an optional trailing slash. A slug is a single path segment. */
const CONTENT_PATH = new RegExp(`^/(${Object.values(PostType).join('|')})/([^/]+)/?$`)
const TYPE_INDEX = 1
const SLUG_INDEX = 2

/** Reads one kind of route from a URL path and its query string, or null when the path is not of that kind. */
type RouteMatcher = (pathname: string, search: string) => Route | null

/** The path without one trailing slash, so `/posts/` reads like `/posts`. */
function trimmedPath(pathname: string): string {
  return pathname.replace(/\/$/, '')
}

function homeRoute(pathname: string): Route | null {
  return pathname === '' || pathname === '/' ? { kind: 'home' } : null
}

/** A route at a fixed path, with or without a trailing slash. */
function fixedRoute(path: string, kind: 'list' | 'books' | 'user'): RouteMatcher {
  return (pathname) => (trimmedPath(pathname) === path ? { kind } : null)
}

/** /write, optionally with ?title=...&slug=... for a post written for something else; both are needed to start from them. */
function writeRoute(pathname: string, search: string): Route | null {
  if (trimmedPath(pathname) !== WRITE_PATH) {
    return null
  }
  const params = new URLSearchParams(search)
  const title = params.get('title')
  const slug = params.get('slug')
  return { kind: 'write', starter: title !== null && slug !== null ? { title, slug } : null }
}

function searchRoute(pathname: string, search: string): Route | null {
  return SEARCH_PATH.test(pathname) ? searchRouteOf(new URLSearchParams(search)) : null
}

/**
 * A route whose path carries a slug: null when the pattern does not match, not found when it matches but the slug
 * cannot be decoded, else the route `toRoute` makes of the match and the decoded slug.
 */
function slugRoute(pattern: RegExp, slugIndex: number, toRoute: (match: RegExpExecArray, slug: string) => Route): RouteMatcher {
  return (pathname) => {
    const match = pattern.exec(pathname)
    if (!match) {
      return null
    }
    const slug = decodedSlug(match[slugIndex])
    return slug === null ? { kind: 'notFound', path: pathname } : toRoute(match, slug)
  }
}

/** The kinds of route in the order they are tried: the first that matches wins. */
const ROUTE_MATCHERS: RouteMatcher[] = [
  homeRoute,
  fixedRoute(LIST_PATH, 'list'),
  writeRoute,
  fixedRoute(BOOKS_PATH, 'books'),
  fixedRoute(USER_PATH, 'user'),
  searchRoute,
  slugRoute(EDIT_PATH, 1, (_match, slug) => ({ kind: 'edit', slug })),
  slugRoute(CONTENT_PATH, SLUG_INDEX, (match, slug) => ({ kind: 'content', type: match[TYPE_INDEX] as PostType, slug })),
]

/** Resolves a URL path such as `/page/about`, with its query string such as `?query=kpi`, to the route it represents. */
export function routeOf(pathname: string, search: string = ''): Route {
  for (const matcher of ROUTE_MATCHERS) {
    const route = matcher(pathname, search)
    if (route !== null) {
      return route
    }
  }
  return { kind: 'notFound', path: pathname }
}

/** Whether the route is the editor, writing a new post or revising one: it takes the whole content area, with no info area. */
export function isEditorRoute(route: Route): boolean {
  return route.kind === 'write' || route.kind === 'edit'
}

/** Whether the info area is shown beside the content: not for the editor, the bookshelf and the personalization page, which take the whole width. */
export function hasAside(route: Route): boolean {
  return !isEditorRoute(route) && route.kind !== 'books' && route.kind !== 'user'
}

/** Builds the URL path for writing a new post, starting from a title and slug when given. */
export function writePath(starter?: PostStarter): string {
  return starter === undefined ? WRITE_PATH : `${WRITE_PATH}?${new URLSearchParams({ title: starter.title, slug: starter.slug })}`
}

/** Whether the info area shows its ad: only beside the home post and a post or page being read. */
export function showsAsideAd(route: Route): boolean {
  return route.kind === 'home' || route.kind === 'content'
}

/** The slug of the post or page being shown, or null when the route is not one (home, a list, or not found). */
export function currentContentSlug(route: Route): string | null {
  return route.kind === 'content' ? route.slug : null
}

/** Where to go back to from the personalization page: the page the visitor came from on this site, else home. */
export function returnPathOf(referrer: string, origin: string): string {
  if (!referrer.startsWith(`${origin}/`)) {
    return '/'
  }
  const path = referrer.slice(origin.length)
  return routeOf(path.split(/[?#]/)[0]).kind === 'user' ? '/' : path
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
