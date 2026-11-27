import { IconDeviceFloppy, IconTrash, IconWorldCheck, IconWorldUpload } from '@tabler/icons-react'
import { IconButton } from 'design-system'

/** 공개 until the post is published, then 공개됨 and disabled. */
function PublishButton({ published, canSave, onPublish }: { published: boolean; canSave: boolean; onPublish: () => void }) {
  return (
    <IconButton
      icon={published ? IconWorldCheck : IconWorldUpload}
      label={published ? '공개됨' : '공개'}
      variant="primary"
      disabled={!canSave || published}
      onClick={onPublish}
    />
  )
}

interface PostEditorActionsProps {
  canSave: boolean
  canRemove: boolean
  published: boolean
  onSave: () => void
  onPublish: () => void
  onRemove: () => void
}

/** The buttons beside the title: 저장, 공개 and 삭제. Publishing is the main action, deleting cannot be undone. */
export function PostEditorActions({ canSave, canRemove, published, onSave, onPublish, onRemove }: PostEditorActionsProps) {
  return (
    <>
      <IconButton icon={IconDeviceFloppy} label="저장" disabled={!canSave} onClick={onSave} />
      <PublishButton published={published} canSave={canSave} onPublish={onPublish} />
      <IconButton icon={IconTrash} label="삭제" variant="danger" disabled={!canRemove} onClick={onRemove} />
    </>
  )
}
