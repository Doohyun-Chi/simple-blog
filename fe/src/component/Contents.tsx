import type { ReactNode } from 'react'
import './Contents.css'

interface ContentsProps {
  children?: ReactNode
}

export function Contents({ children }: ContentsProps) {
  return <main className="contents">{children}</main>
}
