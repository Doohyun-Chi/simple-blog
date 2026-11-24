import { type SubmitEvent, useEffect, useRef, useState } from 'react'
import { chosenSlugOf, cleanImageSlug, IMAGE_SLUG_MAX_LENGTH } from '../model/imageUploadName.ts'
import type { ImageSlugQuestion } from '../model/useImageSlugPrompt.ts'
import './ImageSlugDialog.css'

interface ImageSlugFormProps {
  suggested: string
  onAnswer: (slug: string | null) => void
}

function ImageSlugForm({ suggested, onAnswer }: ImageSlugFormProps) {
  const [typed, updateTyped] = useState(suggested)
  const changed = chosenSlugOf(typed, suggested) !== suggested
  const submit = (event: SubmitEvent) => {
    event.preventDefault()
    onAnswer(chosenSlugOf(typed, suggested))
  }
  return (
    <form className="image-slug-form" onSubmit={submit}>
      <label className="image-slug-label">
        <strong>이미지 slug</strong>
        <input
          className="image-slug-input"
          name="image-slug"
          type="text"
          autoFocus
          maxLength={IMAGE_SLUG_MAX_LENGTH}
          value={typed}
          onChange={(event) => updateTyped(cleanImageSlug(event.target.value))}
        />
      </label>
      <p className="image-slug-note">영문 소문자, 숫자, -만 쓸 수 있습니다. 같은 slug의 내 이미지가 있으면 교체할지 한 번 더 묻습니다.</p>
      <p className="image-slug-note">{changed ? '제안과 다른 slug로 올립니다.' : '제안한 slug 그대로 올립니다.'}</p>
      <div className="image-slug-actions">
        <button type="button" onClick={() => onAnswer(null)}>취소</button>
        <button type="submit">올리기</button>
      </div>
    </form>
  )
}

interface ImageSlugDialogProps {
  question: ImageSlugQuestion | null
  onAnswer: (slug: string | null) => void
}

/**
 * Asks the writer for the slug of an image pasted or dropped into the content, starting from a suggestion, and whether
 * to keep it or change it. 취소 or Esc gives the upload up. It is a native modal dialog, so it sits above the editor,
 * also in full screen.
 */
export function ImageSlugDialog({ question, onAnswer }: ImageSlugDialogProps) {
  const dialog = useRef<HTMLDialogElement>(null)
  const open = question !== null

  useEffect(() => {
    if (open) {
      dialog.current?.showModal()
    } else {
      dialog.current?.close()
    }
  }, [open])

  return (
    <dialog
      ref={dialog}
      className="image-slug-dialog"
      aria-label="이미지 slug"
      onCancel={(event) => {
        event.preventDefault()
        onAnswer(null)
      }}
    >
      {question && <ImageSlugForm key={question.id} suggested={question.suggested} onAnswer={onAnswer} />}
    </dialog>
  )
}
