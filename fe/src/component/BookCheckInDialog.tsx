import { BOOK_INSIGHT_MAX_LENGTH, type Book, todayText } from '../model/Book.ts'
import { saveDatesLabelOf, useCheckInForm } from '../model/useCheckInForm.ts'
import { ModalDialog } from './ModalDialog.tsx'
import './BookCheckInDialog.css'

interface BookCheckInDialogProps {
  book: Book
  onSaved: () => void
  onClose: () => void
}

/** 체크인하기: when the reading started and, if the book is finished, that it is and when; and the insight, which is free text. */
export function BookCheckInDialog({ book, onSaved, onClose }: BookCheckInDialogProps) {
  const form = useCheckInForm(book, onSaved)
  const today = todayText()
  return (
    <ModalDialog label="체크인" onClose={onClose} wide>
      <form className="modal-dialog-form" onSubmit={(event) => event.preventDefault()}>
        <strong>{book.title}</strong>
        <div className="book-check-in-line">
          <input className="modal-dialog-input" type="date" name="reading-start" aria-label="읽기 시작한 날짜" max={today} value={form.start}
            onChange={(event) => form.editStart(event.target.value)} />
          <input className="modal-dialog-input" type="date" name="reading-complete" aria-label="완료일" min={form.start} max={today}
            disabled={!form.completed} value={form.complete} onChange={(event) => form.editComplete(event.target.value)} />
          <label className="book-check-in-completed">
            <input type="checkbox" name="reading-completed" checked={form.completed} onChange={(event) => form.editCompleted(event.target.checked)} /> 완료
          </label>
          <button type="button" className="book-check-in-save" disabled={!form.datesValid || form.busy} onClick={() => void form.saveDates()}>
            {saveDatesLabelOf(book)}
          </button>
        </div>
        {form.lacksThumbnail && (
          <div className="book-check-in-thumbnail">
            <input className="modal-dialog-input" type="url" name="thumbnail-url" aria-label="썸네일 URL" placeholder="썸네일 URL"
              value={form.thumbnailUrl} onChange={(event) => form.editThumbnailUrl(event.target.value)} />
            <button type="button" className="book-check-in-save" disabled={!form.canSaveThumbnail} onClick={() => void form.saveThumbnail()}>
              썸네일 저장
            </button>
          </div>
        )}
        <label className="modal-dialog-label">
          인사이트
          <textarea className="modal-dialog-input book-check-in-insight" name="book-insight" maxLength={BOOK_INSIGHT_MAX_LENGTH} value={form.insight}
            onChange={(event) => form.editInsight(event.target.value)} />
        </label>
        {form.failure !== null && <p className="modal-dialog-failure" role="alert">{form.failure}</p>}
        <div className="modal-dialog-actions">
          <button type="button" disabled={form.busy} onClick={() => void form.saveInsight()}>인사이트 등록</button>
          <button type="button" onClick={onClose}>닫기</button>
        </div>
      </form>
    </ModalDialog>
  )
}
