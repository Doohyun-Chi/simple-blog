import { ButtonWithTooltip, type CodeBlockEditorProps, CodeMirrorEditor, insertCodeBlock$, usePublisher } from '@mdxeditor/editor'
import { IconSitemap } from '@tabler/icons-react'
import { useMermaidSvg } from '../model/useMermaidSvg.ts'
import { useSettledValue } from '../model/useSettledValue.ts'
import './MermaidCodeBlockEditor.css'

/** How long the source must rest before the preview is drawn again. */
const PREVIEW_DELAY_MILLIS = 400

/** What a new diagram starts with. */
const STARTER_DIAGRAM = 'graph TD\n  A[시작] --> B[끝]'

/** The diagram of the source as it is now; source that is not valid Mermaid gets a note only, as the source is right above. */
function MermaidPreview({ source }: { source: string }) {
  const svg = useMermaidSvg(useSettledValue(source, PREVIEW_DELAY_MILLIS))
  switch (svg.state) {
    case 'loading':
      return <div className="mermaid-code-block-preview" aria-busy="true" />
    case 'loaded':
      return <div className="mermaid-code-block-preview" dangerouslySetInnerHTML={{ __html: svg.value }} />
    case 'failed':
      return <div className="mermaid-code-block-preview mermaid-code-block-note" role="status">Mermaid 문법을 확인해 주세요.</div>
  }
}

/**
 * The editor of a ```mermaid block: the source in the same code editor as other code blocks (its language can still be
 * changed there), with the diagram drawn under it as the article will show it.
 */
export function MermaidCodeBlockEditor(props: CodeBlockEditorProps) {
  return (
    <div className="mermaid-code-block">
      <CodeMirrorEditor {...props} />
      <MermaidPreview source={props.code} />
    </div>
  )
}

/** The toolbar button that inserts a ```mermaid block with a small starter diagram. */
export function InsertMermaid() {
  const insertCodeBlock = usePublisher(insertCodeBlock$)
  return (
    <ButtonWithTooltip title="Mermaid 다이어그램" onClick={() => insertCodeBlock({ language: 'mermaid', code: STARTER_DIAGRAM })}>
      <IconSitemap size="1.25em" aria-hidden="true" />
    </ButtonWithTooltip>
  )
}
