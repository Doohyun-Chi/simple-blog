import { hashTagsOf } from '../model/Post.ts'
import './HashTagList.css'

interface HashTagListProps {
  /** The comma separated hash tags as the backend stores them, without the # prefix. */
  hashTags: string
}

/** The hash tags of a post, each with a # prefix. Nothing when there are none. They are not links yet. */
export function HashTagList({ hashTags }: HashTagListProps) {
  const tags = hashTagsOf(hashTags)
  if (tags.length === 0) {
    return null
  }
  return (
    <ul className="hash-tag-list">
      {tags.map((tag) => (
        <li key={tag} className="hash-tag">#{tag}</li>
      ))}
    </ul>
  )
}
