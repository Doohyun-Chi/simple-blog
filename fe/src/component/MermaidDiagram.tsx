import { useMermaidSvg } from '../model/useMermaidSvg.ts'
import './MermaidDiagram.css'

/**
 * A diagram from a ```mermaid block. While it is drawn the space stays empty; source that is not valid Mermaid is shown
 * as the code block it was written as, with a note, so the rest of the post still reads. The SVG comes from Mermaid in
 * strict security mode, which sanitizes the labels itself.
 */
export function MermaidDiagram({ source }: { source: string }) {
  const svg = useMermaidSvg(source)
  switch (svg.state) {
    case 'loading':
      return <div className="mermaid-diagram" aria-busy="true" />
    case 'loaded':
      return <div className="mermaid-diagram" dangerouslySetInnerHTML={{ __html: svg.value }} />
    case 'failed':
      return (
        <figure className="mermaid-diagram-failed">
          <pre><code>{source}</code></pre>
          <figcaption className="mermaid-diagram-note">다이어그램을 그리지 못했습니다. Mermaid 문법을 확인해 주세요.</figcaption>
        </figure>
      )
  }
}
