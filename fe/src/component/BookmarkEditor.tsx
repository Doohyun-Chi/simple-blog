import { Button, Text } from 'design-system'
import type { Bookmark } from '../model/Bookmark.ts'
import { type ArrangementFailure, useBookmarkArrangement } from '../model/useBookmarkArrangement.ts'
import { BookmarkGroup } from './BookmarkGroup.tsx'
import './BookmarkEditor.css'

const REMOVE_QUESTION = '이 책갈피를 삭제할까요? 글은 그대로 남습니다.'

const FAILURE_TEXT: Record<ArrangementFailure, string> = {
  rename: '이름을 바꾸지 못했습니다.',
  remove: '책갈피를 삭제하지 못했습니다.',
  save: '노출 순서를 저장하지 못했습니다.',
}

/** The notes under the page title: what is applied at once and what waits for the save button. */
function EditorNotes() {
  return (
    <div className="bookmark-editor-notes">
      <Text variant="small" tone="tertiary">
        이름 변경과 삭제는 바로 반영됩니다. 이름을 바꾸면 변경 시간이 현재 시간으로 기록됩니다.
      </Text>
      <Text variant="small" tone="tertiary">
        고정, 고정 해제와 순서 변경은 `노출 순서 저장`을 눌러야 반영되며, 이때 변경 시간은 바뀌지 않습니다.
      </Text>
    </div>
  )
}

/** The management page for the loaded bookmarks: the pinned ones above the others, and the button that saves their order. */
export function BookmarkEditor({ initial }: { initial: Bookmark[] }) {
  const arrangement = useBookmarkArrangement(initial)
  const remove = (id: number) => {
    if (window.confirm(REMOVE_QUESTION)) {
      void arrangement.remove(id)
    }
  }
  const shared = {
    disabled: arrangement.busy, onRename: arrangement.rename, onCommitName: (id: number) => void arrangement.commitName(id),
    onPin: arrangement.pin, onUnpin: arrangement.unpin, onRemove: remove, onMove: arrangement.move,
  }
  return (
    <div className="bookmark-editor">
      <h1 className="bookmark-editor-title">책갈피 관리</h1>
      <EditorNotes />
      <BookmarkGroup title="고정 책갈피" empty="고정한 책갈피가 없습니다." bookmarks={arrangement.groups.pinned} pinned {...shared} />
      <BookmarkGroup title="책갈피" empty="아직 책갈피가 없습니다." bookmarks={arrangement.groups.others} pinned={false} {...shared} />
      {arrangement.failure !== null && <Text variant="small" tone="danger" role="alert">{FAILURE_TEXT[arrangement.failure]}</Text>}
      <div className="bookmark-editor-actions">
        <Button disabled={!arrangement.changed || arrangement.busy} onClick={() => void arrangement.save()}>노출 순서 저장</Button>
      </div>
    </div>
  )
}
