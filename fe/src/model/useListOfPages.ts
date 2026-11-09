import { useEffect, useState } from 'react'
import type { PageSummary } from './Page.ts'
import { listOfPages } from './pageApi.ts'

/** Loads the published pages once. On failure the list stays empty so the layout still renders. */
export function useListOfPages(): PageSummary[] {
  const [pages, updatePages] = useState<PageSummary[]>([])

  useEffect(() => {
    const controller = new AbortController()
    listOfPages(controller.signal)
      .then(updatePages)
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          console.error(error)
        }
      })
    return () => controller.abort()
  }, [])

  return pages
}
