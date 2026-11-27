import { IconCheck, IconCopy } from '@tabler/icons-react'
import { Button } from 'design-system'
import { LinkedInLogo } from './LinkedInLogo.tsx'
import { linkedInShareUrl, openLinkedInShare } from '../model/share.ts'
import { useCopyToClipboard } from '../model/useCopyToClipboard.ts'
import './ShareLinks.css'

interface ShareLinksProps {
  /** The link to share. */
  url: string
}

/** 공유하기 copies the link to the clipboard; the LinkedIn logo after it opens LinkedIn's page for writing a post, in a popup. */
export function ShareLinks({ url }: ShareLinksProps) {
  const { copied, copy } = useCopyToClipboard(url)
  return (
    <span className="share-links">
      <Button variant="link" icon={copied ? IconCheck : IconCopy} onClick={copy}>{copied ? '복사됨' : '공유하기'}</Button>
      <a
        className="share-links-linkedin"
        href={linkedInShareUrl(url)}
        target="_blank"
        rel="noopener noreferrer"
        aria-label="LinkedIn에 공유"
        onClick={(event) => {
          event.preventDefault()
          openLinkedInShare(url)
        }}
      >
        <LinkedInLogo />
      </a>
    </span>
  )
}
