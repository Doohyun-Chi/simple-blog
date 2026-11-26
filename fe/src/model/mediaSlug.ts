import { cleanSlug } from './Post.ts'

/** The longest slug of an image; mirrors `MediaRename.slug` in be/apispec.yaml. */
export const MEDIA_SLUG_MAX_LENGTH = 100

const EDGE_DASHES = /^-+|-+$/g

/** Turns typed text into slug characters of an image: lower case letters, digits and `-`. */
export function cleanMediaSlug(text: string): string {
  return cleanSlug(text).toLowerCase().slice(0, MEDIA_SLUG_MAX_LENGTH)
}

/** The slug to send for what was typed: without dashes at its ends, which the backend refuses. */
export function trimmedMediaSlug(typed: string): string {
  return cleanMediaSlug(typed).replace(EDGE_DASHES, '')
}
