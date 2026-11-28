import { IconCheck, IconCopy, IconGripVertical, IconPin, IconPinnedOff, IconTrash } from '@tabler/icons-react'
import { IconButton, TextInput } from 'design-system'
import type { DragEvent } from 'react'
import type { Bookmark } from '../model/Bookmark.ts'
import { PostType, readerSlug } from '../model/Post.ts'
import { contentPath } from '../model/route.ts'
import { useCopyToClipboard } from '../model/useCopyToClipboard.ts'
import './BookmarkRow.css'

interface BookmarkRowProps {
  bookmark: Bookmark
  pinned: boolean
  disabled: boolean
  onRename: (name: string) => void
  /** The name was left (blur or Enter): it is sent if it changed. */
  onCommitName: () => void
  onPin: () => void
  onUnpin: () => void
  onRemove: () => void
  /** Drag and drop of the handle, for a pinned row. */
  drag?: { onStart: (event: DragEvent) => void; onEnd: () => void; onOver: (event: DragEvent) => void; onDrop: (event: DragEvent) => void }
  /** Whether this row is the one being dragged. */
  dragging?: boolean
}

/** The copy icon of the slug; it shows a check for a moment afterwards. */
function CopySlug({ slug }: { slug: string }) {
  const { copied, copy } = useCopyToClipboard(slug)
  const label = copied ? '복사됨' : '슬러그 복사'
  return <IconButton variant="ghost" size="sm" icon={copied ? IconCheck : IconCopy} label={label} onClick={copy} />
}

/** Starts the drag of the handle so that the whole row, not just the handle, is what follows the pointer. */
function startRowDrag(event: DragEvent, onStart: (event: DragEvent) => void) {
  const row = event.currentTarget.closest('li')
  if (row !== null) {
    const box = row.getBoundingClientRect()
    event.dataTransfer.setDragImage(row, event.clientX - box.left, event.clientY - box.top)
  }
  onStart(event)
}

/** The pin icon of an unpinned bookmark, or the pin-off icon and the handle of a pinned one. */
function PinControls({ pinned, disabled, onPin, onUnpin, drag }: Pick<BookmarkRowProps, 'pinned' | 'disabled' | 'onPin' | 'onUnpin' | 'drag'>) {
  if (!pinned) {
    return <IconButton variant="ghost" size="sm" icon={IconPin} label="고정하기" disabled={disabled} onClick={onPin} />
  }
  return (
    <>
      <span className="bookmark-row-handle" draggable role="img" aria-label="순서 바꾸기"
        onDragStart={(event) => drag !== undefined && startRowDrag(event, drag.onStart)} onDragEnd={drag?.onEnd}>
        <IconGripVertical size="1.2em" aria-hidden="true" />
      </span>
      <IconButton variant="ghost" size="sm" icon={IconPinnedOff} label="고정 해제" disabled={disabled} onClick={onUnpin} />
    </>
  )
}

/** One bookmark: its name to edit, the slug linking to the post in another tab with its copy icon, the pin controls and the delete icon. */
export function BookmarkRow(props: BookmarkRowProps) {
  const { bookmark, pinned, disabled, onRename, onCommitName, onPin, onUnpin, onRemove, drag, dragging } = props
  const slug = readerSlug(bookmark.slug)
  return (
    <li className="bookmark-row" data-dragging={dragging === true} onDragOver={drag?.onOver} onDrop={drag?.onDrop}>
      <TextInput className="bookmark-row-name" name={`bookmark-name-${bookmark.id}`} aria-label="책갈피 이름" value={bookmark.name}
        onChange={(event) => onRename(event.target.value)} onBlur={onCommitName}
        onKeyDown={(event) => { if (event.key === 'Enter') { event.currentTarget.blur() } }} />
      <a className="bookmark-row-slug" href={contentPath(PostType.POST, slug)} target="_blank" rel="noopener noreferrer" title={slug}>{slug}</a>
      <CopySlug slug={slug} />
      <PinControls pinned={pinned} disabled={disabled} onPin={onPin} onUnpin={onUnpin} drag={drag} />
      <IconButton variant="ghost" size="sm" icon={IconTrash} label="책갈피 삭제" disabled={disabled} onClick={onRemove} />
    </li>
  )
}
