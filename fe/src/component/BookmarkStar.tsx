import { IconStar, IconStarFilled } from '@tabler/icons-react'
import { IconButton, Text } from 'design-system'
import { useBookmarkToggle } from '../model/useBookmarkToggle.ts'
import './BookmarkStar.css'

interface BookmarkStarProps {
  /** The slug of the post as readers see it. */
  slug: string
  /** The visitor's bookmark of the post, or null when it is not bookmarked. */
  bookmarkId: number | null
  /** Whether the visitor is signed in; a visitor who is not sees the star, which says it needs a sign-in. */
  signedIn: boolean
}

/** The star under the title: filled when the post is bookmarked; pressing it adds or removes the bookmark. */
export function BookmarkStar({ slug, bookmarkId, signedIn }: BookmarkStarProps) {
  const { bookmarked, busy, failed, toggle } = useBookmarkToggle(slug, bookmarkId)
  if (!signedIn) {
    return <IconButton className="bookmark-star" variant="ghost" size="sm" icon={IconStar}
      label="로그인 후 책갈피를 사용할 수 있습니다" disabled />
  }
  return (
    <>
      <IconButton className="bookmark-star" data-bookmarked={bookmarked} variant="ghost" size="sm" icon={bookmarked ? IconStarFilled : IconStar}
        label={bookmarked ? '책갈피 삭제' : '책갈피에 추가'} aria-pressed={bookmarked} disabled={busy} onClick={() => void toggle()} />
      {failed && <Text variant="small" tone="danger" role="alert">책갈피를 바꾸지 못했습니다.</Text>}
    </>
  )
}
