import { type ReactNode, useEffect, useRef } from 'react'
import './ModalDialog.css'

interface ModalDialogProps {
  /** Names the dialog for assistive technology. */
  label: string
  /** Asked to close, by Esc. */
  onClose: () => void
  /** A wide dialog, for one that holds a large text area. */
  wide?: boolean
  children: ReactNode
}

/**
 * A native modal dialog that is open while it is mounted, so what it holds starts fresh each time. It sits above the page
 * and Esc asks `onClose`.
 */
export function ModalDialog({ label, onClose, wide = false, children }: ModalDialogProps) {
  const dialog = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const element = dialog.current
    element?.showModal()
    return () => element?.close()
  }, [])

  return (
    <dialog
      ref={dialog}
      className={wide ? 'modal-dialog modal-dialog-wide' : 'modal-dialog'}
      aria-label={label}
      onCancel={(event) => {
        event.preventDefault()
        onClose()
      }}
    >
      {children}
    </dialog>
  )
}
