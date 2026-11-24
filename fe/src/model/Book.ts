import { ApiError } from './apiClient.ts'
import { cleanSlug } from './Post.ts'

/** Mirrors `Book` in be/bookapispec.yaml. The dates are `yyyy-MM-dd`. */
export interface Book {
  id: number
  title: string
  author: string | null
  bookstoreUrl: string
  thumbnailUrl: string | null
  description: string | null
  insight: string | null
  trackbackSlug: string | null
  registeredDate: string
  readingStartDate: string | null
  readingCompleteDate: string | null
}

/** Mirrors `BookList`: the books asked for, and the years that have books (newest first) for the year filter. */
export interface BookList {
  years: number[]
  books: Book[]
}

/** Mirrors `BookInput`. */
export interface BookInput {
  title: string
  author: string | null
  bookstoreUrl: string
  thumbnailUrl: string | null
  description: string | null
}

/** Mirrors `BookPreview`: what the store page gave; a field it did not give is null. */
export interface BookPreview {
  title: string | null
  author: string | null
  description: string | null
  thumbnailUrl: string | null
}

/** Mirrors `Reading`. A complete date needs a start date. */
export interface Reading {
  readingStartDate: string | null
  readingCompleteDate: string | null
}

/** The most of an insight the shelf shows before it ends with an ellipsis (fe/DESIGN.md). */
export const INSIGHT_SHOWN_LENGTH = 200

/** The limits of `BookInput` and `Insight` in be/bookapispec.yaml. */
export const BOOK_TITLE_MAX_LENGTH = 300
export const BOOK_AUTHOR_MAX_LENGTH = 200
export const BOOK_DESCRIPTION_MAX_LENGTH = 2000
export const BOOK_INSIGHT_MAX_LENGTH = 5000

/** What a review slug starts with (fe/DESIGN.md). */
export const REVIEW_SLUG_PREFIX = 'review-about-'

const REVIEW_SLUG_TITLE_MAX_LENGTH = 100
const DAYS_PER_WEEK = 7
const MONTH_FRACTION_DAY = 1

/** The first 200 characters of the insight, with an ellipsis when there is more. */
export function insightExcerptOf(insight: string): string {
  const characters = Array.from(insight)
  return characters.length <= INSIGHT_SHOWN_LENGTH ? insight : `${characters.slice(0, INSIGHT_SHOWN_LENGTH).join('')}…`
}

/** A book is unread until it has a start date. */
export function isUnread(book: Book): boolean {
  return book.readingStartDate === null
}

function partsOf(date: string): [number, number, number] {
  const [year, month, day] = date.split('-').map(Number)
  return [year, month, day]
}

/** Today as `yyyy-MM-dd` in the reader's time zone. */
export function todayText(now: Date = new Date()): string {
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${String(now.getDate()).padStart(2, '0')}`
}

/** Whole months from `start` to `end`: a month counts once its day of the month is reached. */
function monthsBetween(start: string, end: string): number {
  const [startYear, startMonth, startDay] = partsOf(start)
  const [endYear, endMonth, endDay] = partsOf(end)
  return (endYear - startYear) * 12 + (endMonth - startMonth) - (endDay < startDay ? MONTH_FRACTION_DAY : 0)
}

function daysBetween(start: string, end: string): number {
  const [startYear, startMonth, startDay] = partsOf(start)
  const [endYear, endMonth, endDay] = partsOf(end)
  const millisPerDay = 24 * 60 * 60 * 1000
  return Math.round((Date.UTC(endYear, endMonth - 1, endDay) - Date.UTC(startYear, startMonth - 1, startDay)) / millisPerDay)
}

/** How long the book took, from its start to its completion (or to `today` while it is being read): `N 개월`, or `N 주` under a month. */
export function readingPeriodOf(start: string, complete: string | null, today: string): string {
  const end = complete ?? today
  const months = monthsBetween(start, end)
  if (months >= 1) {
    return `${months} 개월`
  }
  return `${Math.max(1, Math.floor(daysBetween(start, end) / DAYS_PER_WEEK))} 주`
}

/** The slug offered for the review of the book: the prefix and the title's letters and digits, or the book's id when the title has none (Korean). */
export function reviewSlugOf(book: Pick<Book, 'id' | 'title'>): string {
  const words = cleanSlug(book.title.toLowerCase().replace(/[^a-z0-9]+/g, '-')).replace(/^-+|-+$/g, '')
  const base = words.slice(0, REVIEW_SLUG_TITLE_MAX_LENGTH).replace(/-+$/, '')
  return `${REVIEW_SLUG_PREFIX}${base === '' ? `book-${book.id}` : base}`
}

const WEB_URL = /^https?:\/\/\S+$/i

/** Whether the text is an http or https address, which is all the backend takes as a URL of a book. */
export function isWebUrl(text: string): boolean {
  return WEB_URL.test(text.trim())
}

/** The book's details as `BookInput`, with the thumbnail URL replaced: what an edit of only the thumbnail sends. */
export function bookInputWithThumbnail(book: Book, thumbnailUrl: string): BookInput {
  return {
    title: book.title,
    author: book.author,
    bookstoreUrl: book.bookstoreUrl,
    thumbnailUrl: thumbnailUrl.trim(),
    description: book.description,
  }
}

/** What to tell the writer when a shelf request failed, by what the backend answered. */
export function bookFailureText(error: unknown): string {
  switch (error instanceof ApiError ? error.status : 0) {
    case 400:
      return '입력한 내용을 확인해 주세요.'
    case 401:
    case 403:
      return '권한이 없습니다. 다시 로그인해 주세요.'
    case 404:
      return '책을 찾을 수 없습니다.'
    default:
      return '저장하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  }
}

/** What to tell the writer when the store page could not be read: the fields can still be typed. */
export function previewFailureText(error: unknown): string {
  const refused = error instanceof ApiError && error.status === 400
  return refused ? '이 주소는 읽을 수 없습니다. 주소를 확인해 주세요.' : '책 정보를 가져오지 못했습니다. 직접 입력해 주세요.'
}
