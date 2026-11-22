import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { DRAWIO_EDITOR_URL, pngFileOf } from '../model/drawio.ts'
import { useDrawioMessages } from '../model/useDrawioMessages.ts'
import './DrawioDialog.css'

const SAVE_FAILURE = '저장하지 못했습니다. 로그인 상태를 확인하고 다시 저장해 주세요.'

interface DrawioDialogProps {
  /** The diagram to reopen, as a PNG data URI with the diagram inside; null starts an empty one. */
  initialPng: string | null
  /** Keeps the saved PNG (uploads it and puts it in the post); answers whether that worked. */
  onSave: (png: File) => Promise<boolean>
  onClose: () => void
}

/**
 * draw.io's own editor in a modal dialog over the post editor. Saving there hands back a PNG with the diagram inside;
 * once it is kept the dialog closes, and if keeping it fails the dialog stays so the diagram is not lost. It is put at
 * the end of the page body, so opening it from inside the editable area leaves that area untouched.
 */
export function DrawioDialog({ initialPng, onSave, onClose }: DrawioDialogProps) {
  const dialog = useRef<HTMLDialogElement>(null)
  const frame = useRef<HTMLIFrameElement>(null)
  const [note, updateNote] = useState<string | null>(null)
  useEffect(() => {
    dialog.current?.showModal()
  }, [])
  useDrawioMessages(frame, initialPng, {
    onSaved: (png) => {
      updateNote('저장 중…')
      void pngFileOf(png, 'diagram.drawio.png')
        .then(onSave)
        .then((saved) => (saved ? onClose() : updateNote(SAVE_FAILURE)))
    },
    onClosed: onClose,
  })
  return createPortal(
    <dialog ref={dialog} className="drawio-dialog" aria-label="draw.io 다이어그램" onClose={onClose}>
      <div className="drawio-dialog-header">
        <strong>draw.io 다이어그램</strong>
        <span className="drawio-dialog-note" role="status">{note}</span>
        <button type="button" onClick={onClose}>닫기</button>
      </div>
      <iframe ref={frame} className="drawio-dialog-frame" src={DRAWIO_EDITOR_URL} title="draw.io" />
    </dialog>,
    document.body,
  )
}
