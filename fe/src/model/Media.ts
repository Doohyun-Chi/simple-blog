import { ApiError } from './apiClient.ts'

/** Values of `Media.provider` in be/apispec.yaml: an image file on the server, or a reference such as a YouTube URL. */
export const MediaProvider = { INTERNAL: 'internal', EXTERNAL: 'external' } as const
export type MediaProvider = (typeof MediaProvider)[keyof typeof MediaProvider]

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
