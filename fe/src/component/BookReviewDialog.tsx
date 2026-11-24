import type { Book } from '../model/Book.ts'
import { POST_SLUG_MAX_LENGTH } from '../model/Post.ts'
import { useReviewForm } from '../model/useReviewForm.ts'
import { ModalDialog } from './ModalDialog.tsx'

interface BookReviewDialogProps {
  book: Book
  /** The review post is to be written next, starting from this title and slug. */
  onLinked: (starter: { title: string; slug: string }) => void
  onClose: () => void
}

/** 리뷰 적기: asks for the slug of the review post, starting from `review-about-` and the title, and links it to the book. */
export function BookReviewDialog({ book, onLinked, onClose }: BookReviewDialogProps) {
  const form = useReviewForm(book, onLinked)
  return (
    <ModalDialog label="리뷰 적기" onClose={onClose}>
      <form className="modal-dialog-form" onSubmit={(event) => { event.preventDefault(); void form.link() }}>
        <strong>{book.title}</strong>
        <label className="modal-dialog-label">
          리뷰 slug
          <input className="modal-dialog-input" name="review-slug" autoFocus maxLength={POST_SLUG_MAX_LENGTH} value={form.slug}
            onChange={(event) => form.editSlug(event.target.value)} />
        </label>
        <p className="modal-dialog-note">영문, 숫자, -만 쓸 수 있습니다. 저장하면 이 slug로 리뷰 글 작성 화면이 열립니다.</p>
        {form.failure !== null && <p className="modal-dialog-failure" role="alert">{form.failure}</p>}
        <div className="modal-dialog-actions">
          <button type="button" onClick={onClose}>취소</button>
          <button type="submit" disabled={!form.canLink}>저장</button>
        </div>
      </form>
    </ModalDialog>
  )
}
