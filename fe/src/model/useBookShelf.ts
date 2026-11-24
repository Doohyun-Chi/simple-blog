import { useState } from 'react'
import type { BookList } from './Book.ts'
import { listOfBooks } from './bookApi.ts'
import { type Remote, useRemote } from './useRemote.ts'

export interface BookShelf {
  /** The year being shown, or null for All. */
  year: number | null
  selectYear: (year: number | null) => void
  shelf: Remote<BookList>
  /** Loads the shelf again, after a book was registered or changed. */
  refresh: () => void
}

/** The shelf of the year chosen in the year filter (All at first); `refresh` reloads it. */
export function useBookShelf(): BookShelf {
  const [year, selectYear] = useState<number | null>(null)
  const [version, updateVersion] = useState(0)
  const shelf = useRemote(`${year ?? 'all'}:${version}`, (signal) => listOfBooks(year, signal))
  return { year, selectYear, shelf, refresh: () => updateVersion((current) => current + 1) }
}
