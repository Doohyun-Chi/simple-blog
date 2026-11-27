import { IconLayoutGrid, IconList } from '@tabler/icons-react'
import { IconButton } from 'design-system'
import { PostListView } from '../model/Post.ts'

interface PostListViewToggleProps {
  view: PostListView
  onChoose: (view: PostListView) => void
}

/** Two icons, the list and the cards; the chosen one is highlighted. */
export function PostListViewToggle({ view, onChoose }: PostListViewToggleProps) {
  return (
    <div role="group" aria-label="표시 방식">
      <IconButton icon={IconList} label="목록으로 보기" variant={view === PostListView.LIST ? 'primary' : 'ghost'}
        aria-pressed={view === PostListView.LIST} onClick={() => onChoose(PostListView.LIST)} />
      <IconButton icon={IconLayoutGrid} label="카드로 보기" variant={view === PostListView.CARD ? 'primary' : 'ghost'}
        aria-pressed={view === PostListView.CARD} onClick={() => onChoose(PostListView.CARD)} />
    </div>
  )
}
