import type { ReactNode } from 'react'
import './StatusMessage.css'

interface StatusMessageProps {
  children: ReactNode
}

/** A one-line notice in the content area, such as loading, a failure or not found. */
export function StatusMessage({ children }: StatusMessageProps) {
  return <p className="status-message" role="status">{children}</p>
}
