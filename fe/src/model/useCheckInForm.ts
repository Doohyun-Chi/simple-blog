import { useState } from 'react'
import { type Book, bookInputWithThumbnail, isUnread, isWebUrl, todayText } from './Book.ts'
import { checkInBook, deleteBook, saveBookInsight, updateBook } from './bookApi.ts'
import { type BookSubmit, useBookSubmit } from './useBookSubmit.ts'

export interface CheckInForm extends BookSubmit {
  start: string
  editStart: (date: string) => void
  /** Whether the book is marked as finished. */
  completed: boolean
  editCompleted: (completed: boolean) => void
  complete: string
  editComplete: (date: string) => void
  insight: string
  editInsight: (text: string) => void
  /** Whether the dates add up: a start, not in the future, and a completion that is not before it. */
  datesValid: boolean
  /** 체크인 시작: saves the reading dates. */
  saveDates: () => Promise<void>
  /** 인사이트 등록: saves the insight. */
  saveInsight: () => Promise<void>
  /** The thumbnail URL being typed, for a book that has none. */
  thumbnailUrl: string
  editThumbnailUrl: (url: string) => void
  /** Whether the book still lacks a thumbnail, which is when its URL can be given. */
  lacksThumbnail: boolean
  canSaveThumbnail: boolean
  /** Saves the thumbnail URL with `PUT /book/{id}`. */
  saveThumbnail: () => Promise<void>
  /** 삭제: removes the book from the shelf, once `confirmed` says yes. */
  remove: (confirmed: () => boolean) => Promise<void>
}

/** The reading dates and insight of a book being checked in. A book not started yet offers today as its start. */
export function useCheckInForm(book: Book, onSaved: () => void): CheckInForm {
  const today = todayText()
  const [start, editStart] = useState(book.readingStartDate ?? today)
  const [completed, editCompleted] = useState(book.readingCompleteDate !== null)
  const [complete, editComplete] = useState(book.readingCompleteDate ?? today)
  const [insight, editInsight] = useState(book.insight ?? '')
  const [thumbnailUrl, editThumbnailUrl] = useState('')
  const submitter = useBookSubmit(onSaved)
  const datesValid = start !== '' && start <= today && (!completed || (complete >= start && complete <= today))
  return {
    ...submitter,
    start, editStart, completed, editCompleted, complete, editComplete, insight, editInsight, datesValid,
    saveDates: () => submitter.submit(() => checkInBook(book.id, { readingStartDate: start, readingCompleteDate: completed ? complete : null })),
    saveInsight: () => submitter.submit(() => saveBookInsight(book.id, insight)),
    thumbnailUrl,
    editThumbnailUrl,
    lacksThumbnail: book.thumbnailUrl === null,
    canSaveThumbnail: isWebUrl(thumbnailUrl) && !submitter.busy,
    saveThumbnail: () => submitter.submit(() => updateBook(book.id, bookInputWithThumbnail(book, thumbnailUrl))),
    remove: async (confirmed) => {
      if (confirmed()) {
        await submitter.submit(() => deleteBook(book.id))
      }
    },
  }
}

/** The label of the button that saves the reading dates: starting the book, or changing what was saved. */
export function saveDatesLabelOf(book: Book): string {
  return isUnread(book) ? '체크인 시작' : '체크인 저장'
}
