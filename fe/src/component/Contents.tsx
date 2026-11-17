import type { ReactNode } from 'react'
import './Contents.css'

interface ContentsProps {
  /** The reading area, taking all the space the info area leaves. */
  children?: ReactNode
  /** The info area on the right. Without it the reading area takes the whole content area. */
  aside?: ReactNode
}

/** The content area from fe/DESIGN.md: the reading area on the left and, when given, the info area on the right. */
export function Contents({ children, aside }: ContentsProps) {
  return (
    <div className="contents">
      <main className="contents-body">{children}</main>
      {aside && <aside className="contents-aside" aria-label="최근 글">{aside}</aside>}
    </div>
  )
}
