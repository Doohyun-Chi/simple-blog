import { IconCheck, IconCopy } from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { LinkedInLogo } from './LinkedInLogo.tsx'
import { linkedInShareUrl, openLinkedInShare } from '../model/share.ts'
import './ShareLinks.css'

interface ShareLinksProps {
  /** The link to share. */
  url: string
}

/** How long 복사됨 stays after the link was copied. */
const COPIED_MILLIS = 2000

/** 공유하기 copies the link to the clipboard; the LinkedIn logo after it opens LinkedIn's page for writing a post, in a popup. */
export function ShareLinks({ url }: ShareLinksProps) {
  const [copied, setCopied] = useState(false)
  useEffect(() => {
    if (!copied) {
      return
    }
    const timer = setTimeout(() => setCopied(false), COPIED_MILLIS)
    return () => clearTimeout(timer)
  }, [copied])
  const copy = () => {
    navigator.clipboard.writeText(url).then(
      () => setCopied(true),
      () => setCopied(false),
    )
  }
  return (
    <span className="share-links">
      <button type="button" className="share-links-copy" onClick={copy}>
        {copied ? <IconCheck size="1.1em" aria-hidden="true" /> : <IconCopy size="1.1em" aria-hidden="true" />}
        {copied ? '복사됨' : '공유하기'}
      </button>
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
