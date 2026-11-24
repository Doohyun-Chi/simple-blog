import { IconArticle, IconBookOff, IconCalendarCheck, IconPencilPlus } from '@tabler/icons-react'
import { type Book, insightExcerptOf, isUnread, readingPeriodOf, todayText } from '../model/Book.ts'
import { PostType } from '../model/Post.ts'
import { contentPath } from '../model/route.ts'
import './BookCard.css'

interface BookCardProps {
  book: Book
  /** Whether the visitor is signed in: only then are the check-in and the review writing offered. */
  canWrite: boolean
  onCheckIn: (book: Book) => void
  onWriteReview: (book: Book) => void
}

/** The thumbnail, linking to the store page it was registered from in another tab; a large icon stands in when there is none. */
function Thumbnail({ book }: { book: Book }) {
  return (
    <a className="book-card-thumbnail" href={book.bookstoreUrl} target="_blank" rel="noopener noreferrer" title={`${book.title} (판매처)`}>
      {book.thumbnailUrl === null
        ? <IconBookOff className="book-card-no-thumbnail" size="5em" stroke={1} aria-label="썸네일 없음" />
        : <img className="book-card-image" src={book.thumbnailUrl} alt={book.title} referrerPolicy="no-referrer" />}
    </a>
  )
}

/** When the book was started and finished, and how long it took; nothing before it is started. */
function ReadingDates({ book }: { book: Book }) {
  if (book.readingStartDate === null) {
    return null
  }
  const period = readingPeriodOf(book.readingStartDate, book.readingCompleteDate, todayText())
  return (
    <p className="book-card-dates">
      {book.readingStartDate} ~ {book.readingCompleteDate ?? '읽는 중'} ({period})
    </p>
  )
}

/** 리뷰 보기 when the book has a review post; 리뷰 적기, with an icon, for the signed-in writer when it has none. */
function ReviewLink({ book, canWrite, onWriteReview }: Pick<BookCardProps, 'book' | 'canWrite' | 'onWriteReview'>) {
  if (book.trackbackSlug !== null) {
    return <a className="book-card-action" href={contentPath(PostType.POST, book.trackbackSlug)}><IconArticle size="1.1em" aria-hidden="true" /> 리뷰 보기</a>
  }
  return canWrite
    ? <button type="button" className="book-card-action" onClick={() => onWriteReview(book)}><IconPencilPlus size="1.1em" aria-hidden="true" /> 리뷰 적기</button>
    : null
}

/** One book: its thumbnail with the reading dates under it, and beside it the name, description, date, insight and the links. */
export function BookCard({ book, canWrite, onCheckIn, onWriteReview }: BookCardProps) {
  return (
    <li className="book-card">
      <div className="book-card-cover">
        <Thumbnail book={book} />
        <ReadingDates book={book} />
      </div>
      <div className="book-card-body">
        <h3 className="book-card-title">{book.title}</h3>
        {book.author !== null && <p className="book-card-author">{book.author}</p>}
        <p className="book-card-description">{[book.description, book.registeredDate].filter((text) => text !== null).join(' · ')}</p>
        {book.insight !== null && <p className="book-card-insight">{insightExcerptOf(book.insight)}</p>}
        <div className="book-card-actions">
          <ReviewLink book={book} canWrite={canWrite} onWriteReview={onWriteReview} />
          {canWrite && (
            <button type="button" className="book-card-action" onClick={() => onCheckIn(book)}>
              <IconCalendarCheck size="1.1em" aria-hidden="true" /> {isUnread(book) ? '체크인하기' : '체크인 수정'}
            </button>
          )}
        </div>
      </div>
    </li>
  )
}
