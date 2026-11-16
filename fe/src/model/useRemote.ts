import { useEffect, useRef, useState } from 'react'

/** The state of a backend call: still loading, loaded with a value, or failed. */
export type Remote<T> = { state: 'loading' } | { state: 'loaded'; value: T } | { state: 'failed' }

type Settled<T> = { key: string; remote: Remote<T> }

/**
 * Runs `load` whenever `key` changes and reports its progress. A result is kept with the key it was loaded for, so a
 * stale result is never shown for a new key. Only a new `key` reloads, so `load` need not be memoized.
 */
export function useRemote<T>(key: string, load: (signal: AbortSignal) => Promise<T>): Remote<T> {
  const [settled, updateSettled] = useState<Settled<T> | null>(null)
  const latestLoad = useRef(load)

  useEffect(() => {
    latestLoad.current = load
  })

  useEffect(() => {
    const controller = new AbortController()
    latestLoad.current(controller.signal)
      .then((value) => updateSettled({ key, remote: { state: 'loaded', value } }))
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          console.error(error)
          updateSettled({ key, remote: { state: 'failed' } })
        }
      })
    return () => controller.abort()
  }, [key])

  return settled?.key === key ? settled.remote : { state: 'loading' }
}
