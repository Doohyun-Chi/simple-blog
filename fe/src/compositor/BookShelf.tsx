import { IconPlus } from '@tabler/icons-react'
import { Button } from 'design-system'
import { useState } from 'react'
import { BookCard } from '../component/BookCard.tsx'
import { BookCheckInDialog } from '../component/BookCheckInDialog.tsx'
import { BookFormDialog } from '../component/BookFormDialog.tsx'
import { BookReviewDialog } from '../component/BookReviewDialog.tsx'
import { BookYearFilter } from '../component/BookYearFilter.tsx'
import { StatusMessage } from '../component/StatusMessage.tsx'
import type { Book } from '../model/Book.ts'
import { writePath } from '../model/route.ts'
import { useBookShelf } from '../model/useBookShelf.ts'
import './BookShelf.css'

/** The dialog open over the shelf, if any. */
type ShelfDialog = { kind: 'new' } | { kind: 'edit'; book: Book } | { kind: 'checkIn'; book: Book } | { kind: 'review'; book: Book } | null

interface BookShelfProps {
  /** Whether the visitor is signed in: only then can a book be registered, checked in or given a review. */
  canWrite: boolean
}

/**
 * The content area for /books (fe/DESIGN.md): the year filter with 새로운 책 beside it, and the books newest first in two
 * columns. Registering, editing, deleting, checking in and linking a review reload the shelf.
 */
export function BookShelf({ canWrite }: BookShelfProps) {
  const { year, selectYear, shelf, refresh } = useBookShelf()
  const [dialog, openDialog] = useState<ShelfDialog>(null)
  const saved = () => {
    openDialog(null)
    refresh()
  }
  return (
    <section className="book-shelf">
      <div className="book-shelf-header">
        {shelf.state === 'loaded' && <BookYearFilter years={shelf.value.years} year={year} onSelect={selectYear} />}
        {canWrite && (
          <Button variant="link" icon={IconPlus} onClick={() => openDialog({ kind: 'new' })}>새로운 책</Button>
        )}
      </div>
      <Books shelf={shelf} canWrite={canWrite} onEdit={(book) => openDialog({ kind: 'edit', book })} onCheckIn={(book) => openDialog({ kind: 'checkIn', book })}
        onWriteReview={(book) => openDialog({ kind: 'review', book })} />
      <ShelfDialogs dialog={dialog} onSaved={saved} onClose={() => openDialog(null)} />
    </section>
  )
}

type Shelf = ReturnType<typeof useBookShelf>['shelf']

function Books({ shelf, canWrite, onEdit, onCheckIn, onWriteReview }: {
  shelf: Shelf
  canWrite: boolean
  onEdit: (book: Book) => void
  onCheckIn: (book: Book) => void
  onWriteReview: (book: Book) => void
}) {
  switch (shelf.state) {
    case 'loading':
      return <StatusMessage>불러오는 중…</StatusMessage>
    case 'failed':
      return <StatusMessage>서재를 불러오지 못했습니다.</StatusMessage>
    case 'loaded':
      return shelf.value.books.length === 0
        ? <StatusMessage>아직 등록한 책이 없습니다.</StatusMessage>
        : (
          <ul className="book-shelf-books">
            {shelf.value.books.map((book) => (
              <BookCard key={book.id} book={book} canWrite={canWrite} onEdit={onEdit} onCheckIn={onCheckIn} onWriteReview={onWriteReview} />
            ))}
          </ul>
        )
  }
}

/** The dialog that is open, if any. A linked review moves on to writing the review post under its slug. */
function ShelfDialogs({ dialog, onSaved, onClose }: { dialog: ShelfDialog; onSaved: () => void; onClose: () => void }) {
  switch (dialog?.kind) {
    case 'new':
      return <BookFormDialog book={null} onSaved={onSaved} onClose={onClose} />
    case 'edit':
      return <BookFormDialog book={dialog.book} onSaved={onSaved} onClose={onClose} />
    case 'checkIn':
      return <BookCheckInDialog book={dialog.book} onSaved={onSaved} onClose={onClose} />
    case 'review':
      return <BookReviewDialog book={dialog.book} onLinked={(starter) => window.location.assign(writePath(starter))} onClose={onClose} />
    default:
      return null
  }
}
