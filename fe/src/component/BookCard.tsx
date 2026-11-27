import { IconArticle, IconCalendarCheck, IconPencil, IconPencilPlus } from '@tabler/icons-react'
import { Button, buttonClassName, Icon, IconButton } from 'design-system'
import { type Book, insightExcerptOf, isUnread, readingPeriodOf, todayText } from '../model/Book.ts'
import { PostType } from '../model/Post.ts'
import { contentPath } from '../model/route.ts'
import { BookThumbnail } from './BookThumbnail.tsx'
import './BookCard.css'

interface BookCardProps {
  book: Book
  /** Whether the visitor is signed in: only then are the check-in and the review writing offered. */
  canWrite: boolean
  onEdit: (book: Book) => void
  onCheckIn: (book: Book) => void
  onWriteReview: (book: Book) => void
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
    return <a className={buttonClassName({ variant: 'link', size: 'sm' })} href={contentPath(PostType.POST, book.trackbackSlug)}><Icon icon={IconArticle} /> 리뷰 보기</a>
  }
  return canWrite
    ? <Button variant="link" size="sm" icon={IconPencilPlus} onClick={() => onWriteReview(book)}>리뷰 적기</Button>
    : null
}

/** One book: its thumbnail with the reading dates under it, and beside it the name (a link to the store page, with the edit icon), description, date, insight and the links. */
export function BookCard({ book, canWrite, onEdit, onCheckIn, onWriteReview }: BookCardProps) {
  return (
    <li className="book-card">
      <div className="book-card-cover">
        <BookThumbnail title={book.title} thumbnailUrl={book.thumbnailUrl} storeUrl={book.bookstoreUrl} />
        <ReadingDates book={book} />
      </div>
      <div className="book-card-body">
        <h3 className="book-card-title">
          <a className="book-card-title-link" href={book.bookstoreUrl} target="_blank" rel="noopener noreferrer">{book.title}</a>
          {canWrite && (
            <IconButton className="book-card-edit" variant="ghost" size="sm" icon={IconPencil} label="책 수정" onClick={() => onEdit(book)} />
          )}
        </h3>
        {book.author !== null && <p className="book-card-author">{book.author}</p>}
        <p className="book-card-description">{[book.description, book.registeredDate].filter((text) => text !== null).join(' · ')}</p>
        {book.insight !== null && <p className="book-card-insight">{insightExcerptOf(book.insight)}</p>}
        <div className="book-card-actions">
          <ReviewLink book={book} canWrite={canWrite} onWriteReview={onWriteReview} />
          {canWrite && (
            <Button variant="link" size="sm" icon={IconCalendarCheck} onClick={() => onCheckIn(book)}>
              {isUnread(book) ? '체크인하기' : '체크인 수정'}
            </Button>
          )}
        </div>
      </div>
    </li>
  )
}
