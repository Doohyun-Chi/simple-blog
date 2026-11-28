import { BookmarkEditor } from '../component/BookmarkEditor.tsx'
import { StatusMessage } from '../component/StatusMessage.tsx'
import { listOfBookmarks } from '../model/bookmarkApi.ts'
import { useRemote } from '../model/useRemote.ts'

/** The bookmark management page: loads every bookmark of the signed-in user and hands them to the editor. */
export function BookmarkPage() {
  const bookmarks = useRemote('bookmarks', (signal) => listOfBookmarks(undefined, signal))
  switch (bookmarks.state) {
    case 'loading':
      return <StatusMessage>불러오는 중…</StatusMessage>
    case 'failed':
      return <StatusMessage>책갈피를 불러오지 못했습니다.</StatusMessage>
    case 'loaded':
      return <BookmarkEditor initial={bookmarks.value} />
  }
}
