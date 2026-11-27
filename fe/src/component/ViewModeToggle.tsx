import { IconEdit, IconMarkdown } from '@tabler/icons-react'
import { useCellValues, usePublisher, viewMode$ } from '@mdxeditor/editor'
import { IconButton, Text } from 'design-system'
import type { ReactNode } from 'react'
import './ViewModeToggle.css'

interface ViewModeToggleProps {
  /** The toolbar contents of the rich editor; they give way to a title while the Markdown source is shown. */
  children: ReactNode
}

/**
 * Toolbar contents that depend on the mode, and the one button that flips between the rich editor and the Markdown
 * source. Its icon, title and label show the mode it switches to, using Tabler icons. Needs the diffSourcePlugin.
 */
export function ViewModeToggle({ children }: ViewModeToggleProps) {
  const [viewMode] = useCellValues(viewMode$)
  const changeViewMode = usePublisher(viewMode$)
  const source = viewMode === 'source'
  const label = source ? '편집기로 보기' : 'Markdown으로 보기'
  return (
    <>
      {source ? <Text variant="small" tone="tertiary">Markdown 소스</Text> : children}
      <IconButton className="view-mode-toggle" icon={source ? IconEdit : IconMarkdown} label={label} size="sm"
        onClick={() => changeViewMode(source ? 'rich-text' : 'source')} />
    </>
  )
}
