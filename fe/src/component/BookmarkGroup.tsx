import { type DragEvent, useState } from 'react'
import { Text } from 'design-system'
import type { Bookmark } from '../model/Bookmark.ts'
import { BookmarkRow } from './BookmarkRow.tsx'
import './BookmarkGroup.css'

interface BookmarkGroupProps {
  title: string
  /** What a group with no bookmark says. */
  empty: string
  bookmarks: Bookmark[]
  pinned: boolean
  disabled: boolean
  onRename: (id: number, name: string) => void
  onCommitName: (id: number) => void
  onPin: (id: number) => void
  onUnpin: (id: number) => void
  onRemove: (id: number) => void
  /** A pinned bookmark was dropped on another: the first takes the place of the second. */
  onMove: (id: number, targetId: number) => void
}

/** A titled list of bookmarks. The rows of the pinned group can be dragged by their handle. */
export function BookmarkGroup(props: BookmarkGroupProps) {
  const { title, empty, bookmarks, pinned, disabled, onRename, onCommitName, onPin, onUnpin, onRemove, onMove } = props
  const [dragged, updateDragged] = useState<number | null>(null)
  const dragOf = (id: number) => ({
    onStart: (event: DragEvent) => { event.dataTransfer.effectAllowed = 'move'; updateDragged(id) },
    onEnd: () => updateDragged(null),
    onOver: (event: DragEvent) => { if (dragged !== null) { event.preventDefault() } },
    onDrop: (event: DragEvent) => { event.preventDefault(); if (dragged !== null) { onMove(dragged, id) } updateDragged(null) },
  })
  return (
    <section className="bookmark-group">
      <h2 className="bookmark-group-title">{title}</h2>
      {bookmarks.length === 0 && <Text variant="small" tone="tertiary">{empty}</Text>}
      <ul className="bookmark-group-list">
        {bookmarks.map((bookmark) => (
          <BookmarkRow key={bookmark.id} bookmark={bookmark} pinned={pinned} disabled={disabled} drag={pinned ? dragOf(bookmark.id) : undefined}
            dragging={dragged === bookmark.id}
            onRename={(name) => onRename(bookmark.id, name)} onCommitName={() => onCommitName(bookmark.id)} onPin={() => onPin(bookmark.id)}
            onUnpin={() => onUnpin(bookmark.id)} onRemove={() => onRemove(bookmark.id)} />
        ))}
      </ul>
    </section>
  )
}
