import { ApiError, apiUrl, requestJson } from './apiClient.ts'
import { csrfHeaders } from './csrfToken.ts'
import { type ImageSource, type Media, type MediaList, MediaProvider, type UploadProvider } from './Media.ts'

const MEDIA_PATH = '/public/api/v1/media'
const MEDIAS_PATH = '/public/api/v1/medias'

/**
 * Calls `POST /public/api/v1/media` with a file (operationId: createMedia). The type is decided by the backend. An image
 * of the library is internal; one made for the post, such as a draw.io diagram, is content and may come with its source.
 */
export function uploadImage(file: File, provider: UploadProvider = MediaProvider.INTERNAL, source: ImageSource | null = null,
  signal?: AbortSignal): Promise<Media> {
  const body = new FormData()
  body.append('file', file)
  body.append('provider', provider)
  if (source !== null) {
    body.append('source', source.source)
    body.append('sourceType', source.sourceType)
  }
  return requestJson<Media>(MEDIA_PATH, { method: 'POST', headers: csrfHeaders(), body, credentials: 'include', signal })
}

/**
 * Calls `POST /public/api/v1/media` with the file under exactly this slug and `replace` true (operationId: createMedia): the
 * file of the writer's own image with that slug is swapped, and the slug and the URL stay. The image is created when no image has the slug.
 */
export function replaceImage(file: File, slug: string): Promise<Media> {
  const body = new FormData()
  body.append('file', file)
  body.append('provider', MediaProvider.INTERNAL)
  body.append('slug', slug)
  body.append('replace', 'true')
  return requestJson<Media>(MEDIA_PATH, { method: 'POST', headers: csrfHeaders(), body, credentials: 'include' })
}

/** Calls `PUT /public/api/v1/media/{slug}` (operationId: renameMedia): the image gets a new slug; posts that show it by the old one are not changed. */
export function renameMedia(slug: string, newSlug: string, signal?: AbortSignal): Promise<Media> {
  const headers = { 'Content-Type': 'application/json', ...csrfHeaders() }
  return requestJson<Media>(`${MEDIA_PATH}/${slug}`, { method: 'PUT', headers, body: JSON.stringify({ slug: newSlug }), credentials: 'include', signal })
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
  return media.provider === MediaProvider.EXTERNAL ? media.url : apiUrl(media.url)
}

/** Calls `GET /public/api/v1/medias` (operationId: listOfMedias): every media item, newest first. Needs the session. */
export async function listOfMedias(signal?: AbortSignal): Promise<Media[]> {
  const body = await requestJson<MediaList>(MEDIAS_PATH, { signal, credentials: 'include' })
  return body.medias
}

/**
 * Calls `DELETE /public/api/v1/media/{slug}` (operationId: deleteMedia): the item and, for an image, its file are gone for good.
 * Only its owner may; posts that show it are not changed. The response has no body.
 */
export async function deleteMedia(slug: string, signal?: AbortSignal): Promise<void> {
  const response = await fetch(apiUrl(`${MEDIA_PATH}/${slug}`), { method: 'DELETE', headers: csrfHeaders(), credentials: 'include', signal })
  if (!response.ok) {
    throw new ApiError(`DELETE ${MEDIA_PATH}/${slug} failed with ${response.status}`, response.status)
  }
}
