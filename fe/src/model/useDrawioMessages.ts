import { type RefObject, useEffect, useRef } from 'react'
import { DRAWIO_ORIGIN, drawioEventOf, nextDrawioStep } from './drawio.ts'

interface DrawioHandlers {
  /** draw.io saved: the PNG with the diagram inside, as a data URI, and the diagram XML when draw.io sent it. */
  onSaved: (png: string, xml: string | null) => void
  /** draw.io was left without saving. */
  onClosed: () => void
}

/**
 * Speaks the draw.io embed protocol with the editor in `frame`. Messages from any other window or origin are ignored,
 * and answers go only to draw.io's origin.
 */
export function useDrawioMessages(frame: RefObject<HTMLIFrameElement | null>, initialPng: string | null, handlers: DrawioHandlers) {
  const latest = useRef(handlers)
  useEffect(() => {
    latest.current = handlers
  })
  useEffect(() => {
    function receive(message: MessageEvent) {
      const target = frame.current?.contentWindow
      if (message.origin !== DRAWIO_ORIGIN || target === undefined || target === null || message.source !== target) {
        return
      }
      const step = nextDrawioStep(drawioEventOf(message.data), initialPng)
      if (step.kind === 'send') {
        target.postMessage(JSON.stringify(step.message), DRAWIO_ORIGIN)
      } else if (step.kind === 'saved') {
        latest.current.onSaved(step.png, step.xml)
      } else if (step.kind === 'closed') {
        latest.current.onClosed()
      }
    }
    window.addEventListener('message', receive)
    return () => window.removeEventListener('message', receive)
  }, [frame, initialPng])
}
