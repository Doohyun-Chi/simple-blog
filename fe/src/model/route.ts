/** Kinds of content the app can show. Each maps to a top-level path segment: /page/{slug}, /post/{slug}. */
export type ContentType = 'page' | 'post'

export type Route =
  | { kind: 'home' }
  | { kind: 'content'; type: ContentType; slug: string }
  | { kind: 'notFound'; path: string }

/** Matches /page/{slug} and /post/{slug}, with an optional trailing slash. A slug is a single path segment. */
const CONTENT_PATH = /^\/(page|post)\/([^/]+)\/?$/
const TYPE_INDEX = 1
const SLUG_INDEX = 2

/** Resolves a URL path such as `/page/about` to the route it represents. */
export function routeOf(pathname: string): Route {
  if (pathname === '' || pathname === '/') {
    return { kind: 'home' }
  }
  const match = CONTENT_PATH.exec(pathname)
  const slug = match ? decodedSlug(match[SLUG_INDEX]) : null
  if (!match || slug === null) {
    return { kind: 'notFound', path: pathname }
  }
  return { kind: 'content', type: match[TYPE_INDEX] as ContentType, slug }
}

/** The slug of the page being shown, or null when the route is not a page (home, a post, or not found). */
export function currentPageSlug(route: Route): string | null {
  return route.kind === 'content' && route.type === 'page' ? route.slug : null
}

/** Builds the URL path for a piece of content, encoding the slug so non-ASCII slugs stay valid. */
export function contentPath(type: ContentType, slug: string): string {
  return `/${type}/${encodeURIComponent(slug)}`
}

/** Undoes the URL encoding from contentPath, so the slug matches the stored value. A malformed escape yields null. */
function decodedSlug(rawSlug: string): string | null {
  try {
    return decodeURIComponent(rawSlug)
  } catch {
    return null
  }
}
