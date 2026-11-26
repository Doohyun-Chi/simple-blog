import { ApiError } from './apiClient.ts'

/**
 * Values of `Media.provider` in be/apispec.yaml: an image file of the media library, a reference such as a YouTube URL,
 * or an image file made for the content of a post, such as a draw.io diagram, which a post save deletes once unused.
 */
export const MediaProvider = { INTERNAL: 'internal', EXTERNAL: 'external', CONTENT: 'content' } as const
export type MediaProvider = (typeof MediaProvider)[keyof typeof MediaProvider]

/** The providers an uploaded file may ask for: `MediaUpload.provider` in be/apispec.yaml. */
export type UploadProvider = typeof MediaProvider.INTERNAL | typeof MediaProvider.CONTENT

/** Mirrors `Media` in be/apispec.yaml. `url` of an image is a server-relative path; see mediaFileUrl. */
export interface Media {
  slug: string
  provider: MediaProvider
  mimeType: string
  url: string
  thumbnail: string | null
  date: string
}

/** Mirrors `MediaList` in be/apispec.yaml. */
export interface MediaList {
  medias: Media[]
}

/** The largest image the backend accepts, in bytes. Mirrors the 10 MB in be/apispec.yaml; shown, not enforced, here. */
export const MEDIA_MAX_BYTES = 10 * 1024 * 1024

/** Escapes what would end the link text or the image alt text of a Markdown link early. */
function escapedText(text: string): string {
  return text.replace(/[\\[\]]/g, (character) => `\\${character}`)
}

/**
 * The Markdown for a YouTube video: its thumbnail as an image that links to the video, in the standard form
 * `[![title](thumbnail)](https://youtu.be/ID)`. There is no embedded player; a click opens YouTube.
 */
export function youTubeMarkdown(title: string, video: Media): string {
  const text = title.trim() === '' ? video.slug : title.trim()
  return `[![${escapedText(text)}](${video.thumbnail ?? ''})](${video.url})`
}

/** What to tell the writer when an image could not be uploaded, by what the backend answered. */
export function uploadFailureText(error: unknown): string {
  switch (error instanceof ApiError ? error.status : 0) {
    case 401:
    case 403:
      return '이미지를 올릴 권한이 없습니다. 다시 로그인해 주세요.'
    case 422:
      return '이 slug는 교체할 수 없는 항목이 쓰고 있습니다. 다른 slug를 써 주세요.'
    case 413:
      return '이미지는 10MB 이하여야 합니다.'
    case 415:
      return 'PNG, JPEG, GIF, WebP 이미지만 올릴 수 있습니다.'
    default:
      return '이미지를 올리지 못했습니다. 잠시 후 다시 시도해 주세요.'
  }
}

/** The Markdown of an image: `![alt](url)`. The alt text is escaped so it cannot end the image early. */
export function imageMarkdown(alt: string, url: string): string {
  return `![${escapedText(alt)}](${url})`
}

/** Whether the item is a YouTube video (kept by reference) rather than an image file on the server. */
export function isVideo(media: Media): boolean {
  return media.provider === MediaProvider.EXTERNAL
}

/** What to tell the writer when an image could not be renamed or its file replaced, by what the backend answered. */
export function changeFailureText(error: unknown): string {
  switch (error instanceof ApiError ? error.status : 0) {
    case 400:
      return 'slug는 영문 소문자, 숫자, -만 쓸 수 있고 100자까지입니다.'
    case 401:
    case 403:
      return '바꿀 권한이 없습니다. 올린 사람만 바꿀 수 있습니다.'
    case 404:
      return '이미 삭제된 이미지입니다.'
    case 409:
      return '이미 쓰고 있는 slug입니다. 다른 slug를 써 주세요.'
    case 413:
      return '이미지는 10MB 이하여야 합니다.'
    case 415:
      return 'PNG, JPEG, GIF, WebP 이미지만 올릴 수 있습니다.'
    default:
      return '바꾸지 못했습니다. 잠시 후 다시 시도해 주세요.'
  }
}

/** What to tell the writer when a media item could not be deleted, by what the backend answered. */
export function deleteFailureText(error: unknown): string {
  switch (error instanceof ApiError ? error.status : 0) {
    case 401:
    case 403:
      return '삭제할 권한이 없습니다. 올린 사람만 삭제할 수 있습니다.'
    case 404:
      return '이미 삭제된 항목입니다.'
    default:
      return '삭제하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  }
}
