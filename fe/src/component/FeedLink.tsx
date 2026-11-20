import { IconCheck, IconRss } from '@tabler/icons-react'
import { feedUrl } from '../model/feed.ts'
import { useCopyToClipboard } from '../model/useCopyToClipboard.ts'
import './FeedLink.css'

/** The feed logo; selecting it copies the feed's link to the clipboard, and a check mark confirms it for a moment. */
export function FeedLink() {
  const { copied, copy } = useCopyToClipboard(feedUrl())
  return (
    <button
      type="button"
      className="feed-link"
      onClick={copy}
      title="Feed 링크 복사"
      aria-label={copied ? 'Feed 링크 복사됨' : 'Feed 링크 복사'}
    >
      {copied ? <IconCheck size="1.2em" aria-hidden="true" /> : <IconRss size="1.2em" aria-hidden="true" />}
    </button>
  )
}
