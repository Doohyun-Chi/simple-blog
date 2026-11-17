/**
 * Every backend call goes through here, so the per-environment backend endpoint is resolved in one place.
 * The endpoint comes from `VITE_API_BASE_URL` in the `.env.<mode>` files; see `.env.development` and `.env.production`.
 */
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '')

/** Resolves an API path such as `/public/api/v1/pages` against the configured backend endpoint. */
export function apiUrl(path: string): string {
  return `${API_BASE_URL}${path}`
}

/** A non-2xx response. `status` lets callers tell a conflict from a missing resource from a server failure. */
export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

/** Sends a request to the backend and parses the JSON body. Throws an ApiError on a non-2xx response. */
export async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const method = init?.method ?? 'GET'
  const headers = new Headers(init?.headers)
  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json')
  }
  const response = await fetch(apiUrl(path), { ...init, headers })
  if (!response.ok) {
    throw new ApiError(`${method} ${path} failed: ${response.status}`, response.status)
  }
  return (await response.json()) as T
}
