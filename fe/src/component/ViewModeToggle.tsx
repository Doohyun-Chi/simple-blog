import { iconComponentFor$, useCellValues, usePublisher, viewMode$ } from '@mdxeditor/editor'
import type { ReactNode } from 'react'
import './ViewModeToggle.css'

interface ViewModeToggleProps {
  /** The toolbar contents of the rich editor; they give way to a title while the Markdown source is shown. */
  children: ReactNode
}

/**
 * Toolbar contents that depend on the mode, and the one button that flips between the rich editor and the Markdown
 * source. Its icon, title and label show the mode it switches to, using MDXEditor's own icons. Needs the diffSourcePlugin.
 */
export function ViewModeToggle({ children }: ViewModeToggleProps) {
  const [viewMode, iconComponentFor] = useCellValues(viewMode$, iconComponentFor$)
  const changeViewMode = usePublisher(viewMode$)
  const source = viewMode === 'source'
  const label = source ? '편집기로 보기' : 'Markdown으로 보기'
  return (
    <>
      {source ? <span className="view-mode-title">Markdown 소스</span> : children}
      <button className="view-mode-toggle" type="button" title={label} aria-label={label} onClick={() => changeViewMode(source ? 'rich-text' : 'source')}>
        {iconComponentFor(source ? 'rich_text' : 'markdown')}
      </button>
    </>
  )
}
