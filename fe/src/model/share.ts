import { apiUrl } from './apiClient.ts'

/**
 * The link to share for a post or a page: the backend's `/public/share/{slug}` (operationId: shareOfPost), which serves
 * the Open Graph tags link previews are built from and links on to the post view. `slug` is the one readers see, and the
 * result is absolute even when the API base is empty (same origin).
 */
export function shareUrl(slug: string): string {
  return new URL(apiUrl(`/public/share/${encodeURIComponent(slug)}`), window.location.origin).href
}

/** LinkedIn's page for writing a post that shares this URL. */
export function linkedInShareUrl(url: string): string {
  return `https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(url)}`
}

/** Opens LinkedIn's share page in a popup window. */
export function openLinkedInShare(url: string): void {
  window.open(linkedInShareUrl(url), '_blank', 'width=600,height=600,noopener,noreferrer')
}
