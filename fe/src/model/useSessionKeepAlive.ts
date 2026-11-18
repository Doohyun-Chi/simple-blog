import { useRef, useState } from 'react'
import { identity } from './identityApi.ts'

/** How often at most the backend is asked; well inside the backend's one hour session timeout. */
const KEEP_ALIVE_INTERVAL_MS = 5 * 60 * 1000

/**
 * Keeps the session alive while the writer is busy in the editor. The backend ends a session after an hour without
 * a request, and writing a long post sends none, so `touch` (call it on every edit) asks who is signed in, at most
 * once per interval, which starts the hour again. `expired` is true once that answer says nobody is signed in any more.
 */
export function useSessionKeepAlive(): { touch: () => void; expired: boolean } {
  const [expired, updateExpired] = useState(false)
  const lastAsked = useRef(0)

  function touch() {
    const now = Date.now()
    if (now - lastAsked.current < KEEP_ALIVE_INTERVAL_MS) {
      return
    }
    lastAsked.current = now
    identity()
      .then((who) => {
        updateExpired(who === null)
        if (who === null) {
          lastAsked.current = 0
        }
      })
      .catch((error: unknown) => console.error(error))
  }

  return { touch, expired }
}
