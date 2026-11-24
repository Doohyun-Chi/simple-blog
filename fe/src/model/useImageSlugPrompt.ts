import { useCallback, useState } from 'react'

interface PendingImage {
  id: number
  suggested: string
  resolve: (slug: string | null) => void
}

/** The image the writer is being asked about. */
export interface ImageSlugQuestion {
  /** Changes with each image, so a form for the next one starts over. */
  id: number
  suggested: string
}

export interface ImageSlugPrompt {
  /** The image asked about now, or null when nothing waits for an answer. */
  question: ImageSlugQuestion | null
  /** Asks for the slug of an image: the slug chosen, or null when the writer gave the upload up. Several are asked in turn. */
  ask: (suggested: string) => Promise<string | null>
  /** Answers the question shown now: the slug chosen, or null to give the upload up. */
  answer: (slug: string | null) => void
}

let nextId = 0

/** The state of the slug question: the editor asks, the dialog shows it and answers. Images dropped together queue up. */
export function useImageSlugPrompt(): ImageSlugPrompt {
  const [queue, updateQueue] = useState<PendingImage[]>([])
  const ask = useCallback(
    (suggested: string) => new Promise<string | null>((resolve) => {
      updateQueue((pending) => [...pending, { id: nextId++, suggested, resolve }])
    }),
    [],
  )
  const answer = (slug: string | null) => {
    queue[0]?.resolve(slug)
    updateQueue((pending) => pending.slice(1))
  }
  const head = queue[0]
  return { question: head ? { id: head.id, suggested: head.suggested } : null, ask, answer }
}
