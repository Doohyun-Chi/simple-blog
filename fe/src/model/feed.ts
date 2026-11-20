import { apiUrl } from './apiClient.ts'

/** The Atom feed's address: the backend's `/public/feed` (operationId: atomFeed), absolute even when the API base is empty (same origin). */
export function feedUrl(): string {
  return new URL(apiUrl('/public/feed'), window.location.origin).href
}
