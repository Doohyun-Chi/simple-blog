import { IconDeviceFloppy, IconTrash, IconWorldCheck, IconWorldUpload } from '@tabler/icons-react'
import type { ReactNode } from 'react'

interface ActionButtonProps {
  className: string
  label: string
  disabled: boolean
  onClick: () => void
  children: ReactNode
}

/** An icon button: the label is its tooltip and its accessible name. */
function ActionButton({ className, label, disabled, onClick, children }: ActionButtonProps) {
  return (
    <button className={className} type="button" title={label} aria-label={label} disabled={disabled} onClick={onClick}>
      {children}
    </button>
  )
}

/** 공개 until the post is published, then 공개됨 and disabled. */
function PublishButton({ published, canSave, onPublish }: { published: boolean; canSave: boolean; onPublish: () => void }) {
  const Icon = published ? IconWorldCheck : IconWorldUpload
  return (
    <ActionButton
      className="post-editor-button post-editor-publish"
      label={published ? '공개됨' : '공개'}
      disabled={!canSave || published}
      onClick={onPublish}
    >
      <Icon size="1.25em" aria-hidden="true" />
    </ActionButton>
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

/** The buttons beside the title: 저장, 공개 and 삭제. */
export function PostEditorActions({ canSave, canRemove, published, onSave, onPublish, onRemove }: PostEditorActionsProps) {
  return (
    <>
      <ActionButton className="post-editor-button" label="저장" disabled={!canSave} onClick={onSave}>
        <IconDeviceFloppy size="1.25em" aria-hidden="true" />
      </ActionButton>
      <PublishButton published={published} canSave={canSave} onPublish={onPublish} />
      <ActionButton className="post-editor-button post-editor-remove" label="삭제" disabled={!canRemove} onClick={onRemove}>
        <IconTrash size="1.25em" aria-hidden="true" />
      </ActionButton>
    </>
  )
}
