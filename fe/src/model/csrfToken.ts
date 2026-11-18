/** The header the backend expects the CSRF token in, and the cookie it publishes the token in (see be/apispec.yaml). */
export const CSRF_HEADER = 'X-XSRF-TOKEN'
const CSRF_COOKIE = 'XSRF-TOKEN'

/** The token the backend published, or null when it has not been seen yet. */
export function csrfToken(): string | null {
  const prefix = `${CSRF_COOKIE}=`
  const found = document.cookie.split('; ').find((cookie) => cookie.startsWith(prefix))
  return found === undefined ? null : decodeURIComponent(found.slice(prefix.length))
}

/** The header to add to a write request: the token when the backend has published one, nothing otherwise. */
export function csrfHeaders(): Record<string, string> {
  const token = csrfToken()
  return token === null ? {} : { [CSRF_HEADER]: token }
}
