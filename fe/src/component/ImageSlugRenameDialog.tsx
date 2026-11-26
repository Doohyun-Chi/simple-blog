import { type SubmitEvent } from 'react'
import type { Media } from '../model/Media.ts'
import { cleanMediaSlug, MEDIA_SLUG_MAX_LENGTH } from '../model/mediaSlug.ts'
import { useImageSlugRename } from '../model/useImageSlugRename.ts'
import { ModalDialog } from './ModalDialog.tsx'

interface ImageSlugRenameDialogProps {
  slug: string
  onRenamed: (media: Media) => void
  onClose: () => void
}

/** Asks for the new slug of an image of this blog. Posts that show it by the old slug are not changed. */
export function ImageSlugRenameDialog({ slug, onRenamed, onClose }: ImageSlugRenameDialogProps) {
  const form = useImageSlugRename(slug, onRenamed)
  const submit = (event: SubmitEvent) => {
    event.preventDefault()
    if (form.canRename) {
      void form.rename()
    }
  }
  return (
    <ModalDialog label="이미지 slug 변경" onClose={onClose}>
      <form className="modal-dialog-form" onSubmit={submit}>
        <label className="modal-dialog-label">
          이미지 slug
          <input className="modal-dialog-input" name="image-slug" type="text" autoFocus maxLength={MEDIA_SLUG_MAX_LENGTH} value={form.typed}
            onChange={(event) => form.edit(cleanMediaSlug(event.target.value))} />
        </label>
        <p className="modal-dialog-note">영문 소문자, 숫자, -만 쓸 수 있습니다. 다른 글에서 이전 slug로 쓰고 있다면 그 글의 이미지는 더 이상 보이지 않습니다.</p>
        {form.failure !== null && <p className="modal-dialog-failure" role="alert">{form.failure}</p>}
        <div className="modal-dialog-actions">
          <button type="submit" disabled={!form.canRename}>변경</button>
          <button type="button" onClick={onClose}>닫기</button>
        </div>
      </form>
    </ModalDialog>
  )
}
