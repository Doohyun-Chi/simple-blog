import { apiUrl, requestJson } from './apiClient.ts'
import { csrfHeaders } from './csrfToken.ts'
import type { Media, MediaList } from './Media.ts'

const MEDIA_PATH = '/public/api/v1/media'
const MEDIAS_PATH = '/public/api/v1/medias'

/** Calls `POST /public/api/v1/media` with a file (operationId: createMedia). The type is decided by the backend. */
export function uploadImage(file: File, signal?: AbortSignal): Promise<Media> {
  const body = new FormData()
  body.append('file', file)
  return requestJson<Media>(MEDIA_PATH, { method: 'POST', headers: csrfHeaders(), body, credentials: 'include', signal })
}

/** Calls `POST /public/api/v1/media` with a YouTube link (operationId: createMedia). Nothing is embedded or fetched. */
export function registerVideo(url: string, signal?: AbortSignal): Promise<Media> {
  const headers = { 'Content-Type': 'application/json', ...csrfHeaders() }
  return requestJson<Media>(MEDIA_PATH, { method: 'POST', headers, body: JSON.stringify({ url }), credentials: 'include', signal })
}

/** The slug of an internal image of this blog from where it is loaded (see mediaFileUrl), or null for any other source. */
export function mediaSlugOf(source: string): string | null {
  const prefix = apiUrl(`${MEDIA_PATH}/`)
  return source.startsWith(prefix) && source.length > prefix.length ? source.slice(prefix.length) : null
}

/** Where an image can be loaded from: the server-relative path of an internal item resolved against the backend. */
export function mediaFileUrl(media: Media): string {
  return media.provider === 'internal' ? apiUrl(media.url) : media.url
}

/** Calls `GET /public/api/v1/medias` (operationId: listOfMedias): every media item, newest first. Needs the session. */
export async function listOfMedias(signal?: AbortSignal): Promise<Media[]> {
  const body = await requestJson<MediaList>(MEDIAS_PATH, { signal, credentials: 'include' })
  return body.medias
}
