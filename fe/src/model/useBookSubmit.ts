import { useState } from 'react'
import { bookFailureText } from './Book.ts'

export interface BookSubmit {
  busy: boolean
  /** What to tell the writer when the last submit failed; null otherwise. */
  failure: string | null
  /** Runs the request; when it succeeds `onDone` is called, when it fails the failure is kept to show. */
  submit: (request: () => Promise<unknown>) => Promise<void>
}

/** The state of a dialog's save: while it runs the buttons wait, and a failure is kept for the dialog to show. */
export function useBookSubmit(onDone: () => void): BookSubmit {
  const [busy, updateBusy] = useState(false)
  const [failure, updateFailure] = useState<string | null>(null)
  async function submit(request: () => Promise<unknown>) {
    updateBusy(true)
    updateFailure(null)
    try {
      await request()
      onDone()
    } catch (error: unknown) {
      console.error(error)
      updateFailure(bookFailureText(error))
    } finally {
      updateBusy(false)
    }
  }
  return { busy, failure, submit }
}
