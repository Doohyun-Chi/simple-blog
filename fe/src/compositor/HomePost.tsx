import type { ReactNode } from 'react'
import { SiteTitle } from '../component/SiteTitle.tsx'
import { StatusMessage } from '../component/StatusMessage.tsx'
import { type PostSummary, readerSlug } from '../model/Post.ts'
import type { Remote } from '../model/useRemote.ts'
import { PostReader } from './PostReader.tsx'

interface HomePostProps {
  recent: Remote<PostSummary[]>
  /** The user's main title, the web page title while no post is open. */
  siteName: string
  signedIn: boolean
}

/** What is shown while no post is: the site name as the web page title, and the message if any. */
function WithoutPost({ siteName, children }: { siteName: string; children?: ReactNode }) {
  return (
    <>
      <SiteTitle name={siteName} />
      {children}
    </>
  )
}

/** The reading area for the home route: the body of the newest post (fe/DESIGN.md). */
export function HomePost({ recent, siteName, signedIn }: HomePostProps) {
  switch (recent.state) {
    case 'loading':
      return <WithoutPost siteName={siteName}><StatusMessage>불러오는 중…</StatusMessage></WithoutPost>
    case 'failed':
      return <WithoutPost siteName={siteName}><StatusMessage>글을 불러오지 못했습니다.</StatusMessage></WithoutPost>
    case 'loaded':
      return recent.value.length === 0
        ? <WithoutPost siteName={siteName} />
        : <PostReader slug={readerSlug(recent.value[0].slug)} siteName={siteName} signedIn={signedIn} />
  }
}
