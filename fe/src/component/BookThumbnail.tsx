import { IconBookOff } from '@tabler/icons-react'
import './BookThumbnail.css'

interface BookThumbnailProps {
  title: string
  /** The image's address; none (or a blank one) shows a large icon in its place. */
  thumbnailUrl: string | null
  /** The store page it links to in another tab. */
  storeUrl: string
}

/** The thumbnail of a book, 5em tall and as wide as it is in proportion, linking to the store page; shared by the card and the form. */
export function BookThumbnail({ title, thumbnailUrl, storeUrl }: BookThumbnailProps) {
  const missing = thumbnailUrl === null || thumbnailUrl.trim() === ''
  return (
    <a className="book-thumbnail" href={storeUrl} target="_blank" rel="noopener noreferrer" title={`${title} (판매처)`}>
      {missing
        ? <IconBookOff className="book-thumbnail-none" size="5em" stroke={1} aria-label="썸네일 없음" />
        : <img className="book-thumbnail-image" src={thumbnailUrl} alt={title} referrerPolicy="no-referrer" />}
    </a>
  )
}
