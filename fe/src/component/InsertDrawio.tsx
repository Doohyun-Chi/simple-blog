import { ButtonWithTooltip, insertMarkdown$, usePublisher } from '@mdxeditor/editor'
import { IconVectorBezier2 } from '@tabler/icons-react'
import { useState } from 'react'
import { markdownOfMedia } from '../model/mediaMarkdown.ts'
import { uploadImage } from '../model/mediaApi.ts'
import { DrawioDialog } from './DrawioDialog.tsx'

/** The alt text of a diagram drawn here. */
export const DRAWIO_ALT = 'draw.io 다이어그램'

/** The toolbar button that draws a new diagram in draw.io and puts it in the post as an image, uploaded like any other. */
export function InsertDrawio() {
  const insertMarkdown = usePublisher(insertMarkdown$)
  const [open, updateOpen] = useState(false)
  async function keep(png: File): Promise<boolean> {
    try {
      insertMarkdown(markdownOfMedia(await uploadImage(png), DRAWIO_ALT))
      return true
    } catch (error: unknown) {
      console.error(error)
      return false
    }
  }
  return (
    <>
      <ButtonWithTooltip title="draw.io 다이어그램" onClick={() => updateOpen(true)}>
        <IconVectorBezier2 size="1.25em" aria-hidden="true" />
      </ButtonWithTooltip>
      {open && <DrawioDialog initialPng={null} onSave={keep} onClose={() => updateOpen(false)} />}
    </>
  )
}
