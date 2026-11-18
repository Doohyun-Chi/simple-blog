/** Mirrors `Media` in be/apispec.yaml. `url` of an image is a server-relative path; see mediaFileUrl. */
export interface Media {
  slug: string
  provider: 'internal' | 'external'
  mimeType: string
  url: string
  thumbnail: string | null
  date: string
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
