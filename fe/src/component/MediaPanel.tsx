import { iconComponentFor$, insertMarkdown$, useCellValue, usePublisher } from '@mdxeditor/editor'
import { useEffect, useRef, useState } from 'react'
import { MediaPanelBody } from './MediaPanelBody.tsx'
import './MediaPanel.css'

/**
 * The editor's media button and the panel it opens: upload a new image or add a YouTube video, or browse the media
 * already registered and pick one. It is a native modal dialog, so it sits above the editor, also in full screen.
 * Its body is mounted only while it is open, so the gallery is loaded fresh each time.
 */
export function MediaPanel() {
  const iconComponentFor = useCellValue(iconComponentFor$)
  const insertMarkdown = usePublisher(insertMarkdown$)
  const dialog = useRef<HTMLDialogElement>(null)
  const [open, updateOpen] = useState(false)

  useEffect(() => {
    if (open) {
      dialog.current?.showModal()
    } else {
      dialog.current?.close()
    }
  }, [open])

  return (
    <>
      <button className="media-panel-button" type="button" title="이미지·영상" aria-label="이미지·영상" onClick={() => updateOpen(true)}>
        {iconComponentFor('add_photo')}
      </button>
      <dialog ref={dialog} className="media-panel" aria-label="이미지·영상" onClose={() => updateOpen(false)}>
        <div className="media-panel-header">
          <strong>이미지·영상</strong>
          <button type="button" onClick={() => updateOpen(false)}>닫기</button>
        </div>
        {open && <MediaPanelBody onInsert={(markdown) => { insertMarkdown(markdown); updateOpen(false) }} />}
      </dialog>
    </>
  )
}
