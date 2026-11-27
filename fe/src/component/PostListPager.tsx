import { IconChevronLeft, IconChevronRight } from '@tabler/icons-react'
import { Button } from 'design-system'
import './PostListPager.css'

interface PostListPagerProps {
  hasPrevious: boolean
  hasNext: boolean
  onPrevious: () => void
  onNext: () => void
}

/** PREV before the first page is left out, and so is NEXT after the last one. */
export function PostListPager({ hasPrevious, hasNext, onPrevious, onNext }: PostListPagerProps) {
  if (!hasPrevious && !hasNext) {
    return null
  }
  return (
    <nav className="post-list-pager" aria-label="글 목록 탐색">
      {hasPrevious && <Button className="post-list-pager-previous" variant="link" icon={IconChevronLeft} onClick={onPrevious}>PREV</Button>}
      {hasNext && <Button className="post-list-pager-next" variant="link" icon={IconChevronRight} onClick={onNext}>NEXT</Button>}
    </nav>
  )
}
