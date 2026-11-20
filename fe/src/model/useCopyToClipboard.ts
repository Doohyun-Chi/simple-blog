import { useEffect, useState } from 'react'

/** How long the copied state stays after the text was copied. */
const COPIED_MILLIS = 2000

/** `copy` puts the text on the clipboard; `copied` is true for a moment afterwards, so the caller can confirm it. */
export function useCopyToClipboard(text: string): { copied: boolean; copy: () => void } {
  const [copied, setCopied] = useState(false)
  useEffect(() => {
    if (!copied) {
      return
    }
    const timer = setTimeout(() => setCopied(false), COPIED_MILLIS)
    return () => clearTimeout(timer)
  }, [copied])
  const copy = () => {
    navigator.clipboard.writeText(text).then(
      () => setCopied(true),
      () => setCopied(false),
    )
  }
  return { copied, copy }
}
