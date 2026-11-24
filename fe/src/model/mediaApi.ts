import { ApiError, apiUrl, requestJson } from './apiClient.ts'
import { csrfHeaders } from './csrfToken.ts'
import { ImageUploadCancelled } from './imageUploadName.ts'
import { type Media, type MediaList, MediaProvider, type UploadProvider } from './Media.ts'

const MEDIA_PATH = '/public/api/v1/media'
const MEDIAS_PATH = '/public/api/v1/medias'

/**
 * Calls `POST /public/api/v1/media` with a file (operationId: createMedia). The type is decided by the backend. An image
 * of the library is internal; one made for the post, such as a draw.io diagram, is content.
 */
export function uploadImage(file: File, provider: UploadProvider = MediaProvider.INTERNAL, signal?: AbortSignal): Promise<Media> {
  const body = new FormData()
  body.append('file', file)
  body.append('provider', provider)
  return requestJson<Media>(MEDIA_PATH, { method: 'POST', headers: csrfHeaders(), body, credentials: 'include', signal })
}

function slugUpload(file: File, slug: string, replace: boolean): Promise<Media> {
  const body = new FormData()
  body.append('file', file)
  body.append('provider', MediaProvider.INTERNAL)
  body.append('slug', slug)
  body.append('replace', String(replace))
  return requestJson<Media>(MEDIA_PATH, { method: 'POST', headers: csrfHeaders(), body, credentials: 'include' })
}

/**
 * Calls `POST /public/api/v1/media` with the file under exactly this slug (operationId: createMedia). When the writer's own
 * image already has the slug (409), `confirmReplace` is asked, and the file is sent again as a replacement if it says yes;
 * if it says no the upload is cancelled with ImageUploadCancelled.
 */
export async function uploadImageAs(file: File, slug: string, confirmReplace: (slug: string) => boolean): Promise<Media> {
  try {
    return await slugUpload(file, slug, false)
  } catch (error: unknown) {
    if (!(error instanceof ApiError && error.status === 409)) {
      throw error
    }
    if (!confirmReplace(slug)) {
      throw new ImageUploadCancelled()
    }
    return slugUpload(file, slug, true)
  }
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
