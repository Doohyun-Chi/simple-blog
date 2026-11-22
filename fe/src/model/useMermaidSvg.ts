import { mermaidSvgOf } from './mermaid.ts'
import { type Remote, useRemote } from './useRemote.ts'

/** The SVG of a Mermaid diagram, drawn again whenever its source changes. */
export function useMermaidSvg(source: string): Remote<string> {
  return useRemote(source, () => mermaidSvgOf(source))
}
