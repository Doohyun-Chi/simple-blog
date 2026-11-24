import { cleanSlug } from './Post.ts'

/**
 * The slug an image of the editor is uploaded under. A pasted image, which every browser calls `image.png`, and a dropped
 * file with a Korean name say nothing as a slug, so the writer is asked for one, starting from the suggestion here. The
 * backend keeps the slug exactly as sent (be/apispec.yaml, `MediaUpload.slug`).
 */

/** The longest slug the writer can give; the backend cuts the name at this length. */
export const IMAGE_SLUG_MAX_LENGTH = 100

const GENERIC_NAME = /^(image|blob|unnamed|untitled)\d*$/
const NON_ALPHANUMERIC = /[^a-z0-9]+/g
const EDGE_DASHES = /^-+|-+$/g

/** The part of the file name before its extension, as slug characters. */
function slugBaseOf(name: string): string {
  const dot = name.lastIndexOf('.')
  return (dot > 0 ? name.slice(0, dot) : name).toLowerCase().replace(NON_ALPHANUMERIC, '-').replace(EDGE_DASHES, '')
}

function twoDigits(value: number): string {
  return String(value).padStart(2, '0')
}

/** `20261124-103015` for the local time, so images pasted one after another read in order. */
function stampOf(now: Date): string {
  const day = `${now.getFullYear()}${twoDigits(now.getMonth() + 1)}${twoDigits(now.getDate())}`
  return `${day}-${twoDigits(now.getHours())}${twoDigits(now.getMinutes())}${twoDigits(now.getSeconds())}`
}

/** The slug offered for the file: its own name when that is readable, otherwise one made from the time. */
export function suggestedSlugOf(file: File, now: Date = new Date()): string {
  const base = slugBaseOf(file.name).slice(0, IMAGE_SLUG_MAX_LENGTH).replace(EDGE_DASHES, '')
  return base === '' || GENERIC_NAME.test(base) ? `image-${stampOf(now)}` : base
}

/** Turns typed text into slug characters: lower case letters, digits and `-`. */
export function cleanImageSlug(text: string): string {
  return cleanSlug(text).toLowerCase().slice(0, IMAGE_SLUG_MAX_LENGTH)
}

/** The slug to upload with: what was typed without dashes at its ends, or the suggestion when nothing usable is left. */
export function chosenSlugOf(typed: string, suggested: string): string {
  const slug = cleanImageSlug(typed).replace(EDGE_DASHES, '')
  return slug === '' ? suggested : slug
}

/** Thrown to the editor when the writer gave an image up in the slug dialog: nothing was uploaded and nothing failed. */
export class ImageUploadCancelled extends Error {
  constructor() {
    super('Image upload cancelled')
  }
}
