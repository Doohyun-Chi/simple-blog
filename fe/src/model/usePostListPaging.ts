import { useState } from 'react'
import { PostListView } from './Post.ts'
import { LIST_POST_LIMIT } from './useListOfPosts.ts'

const VIEW_STORAGE_KEY = 'post-list-view'

/** The view the reader chose last; the list view when nothing is kept or the storage cannot be read. */
function storedView(): PostListView {
  try {
    return window.localStorage.getItem(VIEW_STORAGE_KEY) === PostListView.CARD ? PostListView.CARD : PostListView.LIST
  } catch {
    return PostListView.LIST
  }
}

function keepView(view: PostListView) {
  try {
    window.localStorage.setItem(VIEW_STORAGE_KEY, view)
  } catch {
    // The choice just is not kept.
  }
}

/** Where the reader is in a list: the view and the offset of its page. The offset starts over when `key` (the search) changes. */
export function usePostListPaging(key: string) {
  const [view, updateView] = useState<PostListView>(storedView)
  const [position, updatePosition] = useState({ key, offset: 0 })
  const offset = position.key === key ? position.offset : 0
  return {
    view,
    offset,
    hasPrevious: offset > 0,
    showNext: () => updatePosition({ key, offset: offset + LIST_POST_LIMIT }),
    showPrevious: () => updatePosition({ key, offset: Math.max(0, offset - LIST_POST_LIMIT) }),
    chooseView: (next: PostListView) => {
      keepView(next)
      updateView(next)
      updatePosition({ key, offset: 0 })
    },
  }
}
