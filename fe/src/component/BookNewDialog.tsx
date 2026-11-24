import { IconBookOff } from '@tabler/icons-react'
import {
  BOOK_AUTHOR_MAX_LENGTH,
  BOOK_DESCRIPTION_MAX_LENGTH,
  BOOK_TITLE_MAX_LENGTH,
} from '../model/Book.ts'
import { type NewBookForm, useNewBookForm } from '../model/useNewBookForm.ts'
import { ModalDialog } from './ModalDialog.tsx'

interface BookNewDialogProps {
  onRegistered: () => void
  onClose: () => void
}

/** 새로운 책: the store page's URL is read, then the thumbnail, title, author and description it gave are shown to be edited and registered. */
export function BookNewDialog({ onRegistered, onClose }: BookNewDialogProps) {
  const form = useNewBookForm(onRegistered)
  return (
    <ModalDialog label="새로운 책" onClose={onClose}>
      <form className="modal-dialog-form" onSubmit={(event) => { event.preventDefault(); void form.register() }}>
        <label className="modal-dialog-label">
          <strong>책 상세 정보 URL</strong>
          <input className="modal-dialog-input" type="url" name="book-url" autoFocus value={form.url}
            onChange={(event) => form.editUrl(event.target.value)} placeholder="https://www.yes24.com/Product/Goods/..." />
        </label>
        <div className="modal-dialog-actions">
          <button type="button" disabled={form.fetching || form.url.trim() === ''} onClick={() => void form.fetchPreview()}>
            {form.fetching ? '가져오는 중…' : '정보 가져오기'}
          </button>
        </div>
        {form.previewFailure !== null && <p className="modal-dialog-failure" role="alert">{form.previewFailure}</p>}
        {form.fields !== null && <BookFields form={form} />}
        {form.failure !== null && <p className="modal-dialog-failure" role="alert">{form.failure}</p>}
        <div className="modal-dialog-actions">
          <button type="button" onClick={onClose}>취소</button>
          <button type="submit" disabled={!form.canRegister}>확인</button>
        </div>
      </form>
    </ModalDialog>
  )
}

/** The thumbnail (a large icon when there is none) and the fields the writer can edit. */
function BookFields({ form }: { form: NewBookForm }) {
  const fields = form.fields
  if (fields === null) {
    return null
  }
  return (
    <>
      {fields.thumbnailUrl === null
        ? <IconBookOff size="6em" stroke={1} aria-label="썸네일 없음" />
        : <img src={fields.thumbnailUrl} alt={fields.title} height={120} referrerPolicy="no-referrer" />}
      <label className="modal-dialog-label">
        <strong>제목</strong>
        <input className="modal-dialog-input" name="book-title" maxLength={BOOK_TITLE_MAX_LENGTH} value={fields.title}
          onChange={(event) => form.editFields({ title: event.target.value })} />
      </label>
      <label className="modal-dialog-label">
        <strong>저자</strong>
        <input className="modal-dialog-input" name="book-author" maxLength={BOOK_AUTHOR_MAX_LENGTH} value={fields.author ?? ''}
          onChange={(event) => form.editFields({ author: event.target.value })} />
      </label>
      <label className="modal-dialog-label">
        <strong>소개글</strong>
        <textarea className="modal-dialog-input" name="book-description" rows={4} maxLength={BOOK_DESCRIPTION_MAX_LENGTH}
          value={fields.description ?? ''} onChange={(event) => form.editFields({ description: event.target.value })} />
      </label>
    </>
  )
}
