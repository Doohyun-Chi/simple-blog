import { mediaSlugOf } from './mediaApi.ts'

/** Where draw.io's editor is served for embedding. Only messages from this origin are listened to. */
export const DRAWIO_ORIGIN = 'https://embed.diagrams.net'

/** draw.io in embed mode speaking JSON messages, in Korean, with one 저장 후 닫기 button instead of separate save and exit. */
export const DRAWIO_EDITOR_URL = `${DRAWIO_ORIGIN}/?embed=1&proto=json&spin=1&saveAndExit=1&noSaveBtn=1&lang=ko`

/** What the page does after a message from draw.io: answer it, take the saved PNG, close, or nothing. */
export type DrawioStep =
  | { kind: 'send'; message: Record<string, unknown> }
  | { kind: 'saved'; png: string }
  | { kind: 'closed' }
  | { kind: 'ignore' }

/** A message from draw.io as sent in proto=json mode: a JSON string with an `event`. Anything else is not one. */
export function drawioEventOf(data: unknown): Record<string, unknown> | null {
  if (typeof data !== 'string') {
    return null
  }
  try {
    const value: unknown = JSON.parse(data)
    return typeof value === 'object' && value !== null ? (value as Record<string, unknown>) : null
  } catch {
    return null
  }
}

/**
 * The embed protocol: when draw.io is ready it gets the diagram, a PNG with the diagram inside or an empty one; a save
 * asks for that PNG back (`xmlpng`), which is what gets uploaded; an exit closes the editor without saving.
 */
export function nextDrawioStep(event: Record<string, unknown> | null, initialPng: string | null): DrawioStep {
  switch (event?.event) {
    case 'init':
      return { kind: 'send', message: { action: 'load', autosave: 0, ...(initialPng === null ? { xml: '' } : { xmlpng: initialPng }) } }
    case 'save':
      return { kind: 'send', message: { action: 'export', format: 'xmlpng', spinKey: 'saving' } }
    case 'export':
      return typeof event.data === 'string' ? { kind: 'saved', png: event.data } : { kind: 'ignore' }
    case 'exit':
      return { kind: 'closed' }
    default:
      return { kind: 'ignore' }
  }
}

const PNG_SIGNATURE = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]

/** The PNG chunks that hold text; draw.io keeps the diagram in one whose keyword is `mxfile`. */
const TEXT_CHUNKS = new Set(['tEXt', 'zTXt', 'iTXt'])

const DRAWIO_KEYWORD = 'mxfile\0'

function asciiOf(bytes: Uint8Array, offset: number, length: number): string {
  return String.fromCharCode(...bytes.subarray(offset, offset + length))
}

/** Whether a PNG carries a draw.io diagram, as one exported with "Include a copy of my diagram" does. */
export function isDrawioPng(bytes: Uint8Array): boolean {
  if (!PNG_SIGNATURE.every((byte, index) => bytes[index] === byte)) {
    return false
  }
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength)
  for (let offset = PNG_SIGNATURE.length; offset + 8 <= bytes.length; offset += 12 + view.getUint32(offset)) {
    const type = asciiOf(bytes, offset + 4, 4)
    if (TEXT_CHUNKS.has(type) && asciiOf(bytes, offset + 8, DRAWIO_KEYWORD.length) === DRAWIO_KEYWORD) {
      return true
    }
  }
  return false
}

function dataUriOf(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(blob)
  })
}

/**
 * The image as a PNG data URI for draw.io to reopen, or null when it is not a draw.io PNG of this blog. Only images of
 * this blog's media store are read to look for a diagram.
 */
export async function drawioPngOf(source: string, signal?: AbortSignal): Promise<string | null> {
  if (mediaSlugOf(source) === null) {
    return null
  }
  const response = await fetch(source, { signal })
  const blob = await response.blob()
  return response.ok && isDrawioPng(new Uint8Array(await blob.arrayBuffer())) ? dataUriOf(blob) : null
}

/** The PNG draw.io exported, as a file to upload. */
export async function pngFileOf(dataUri: string, name: string): Promise<File> {
  const blob = await (await fetch(dataUri)).blob()
  return new File([blob], name, { type: 'image/png' })
}
