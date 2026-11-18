import { imageMarkdown, isVideo, type Media, youTubeMarkdown } from './Media.ts'
import { mediaFileUrl } from './mediaApi.ts'

/** The Markdown that puts a media item into a post: the image itself, or a YouTube thumbnail linking to the video. */
export function markdownOfMedia(media: Media, title: string): string {
  const text = title.trim() === '' ? media.slug : title.trim()
  return isVideo(media) ? youTubeMarkdown(text, media) : imageMarkdown(text, mediaFileUrl(media))
}

/** The picture a gallery tile shows: the image file, or the thumbnail of a video. */
export function tilePictureUrl(media: Media): string {
  return isVideo(media) ? (media.thumbnail ?? '') : mediaFileUrl(media)
}
