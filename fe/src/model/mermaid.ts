import type { Element, ElementContent } from 'hast'
import type { MermaidConfig } from 'mermaid'

/** The class the Markdown renderer gives the code of a ```mermaid block. */
const MERMAID_CLASS = 'language-mermaid'

/** Mermaid theme variables and the CSS tokens of index.css they take their value from, so colours stay in the CSS. */
const THEME_TOKENS: Record<string, string> = {
  fontFamily: '--font-main',
  background: '--paper',
  primaryColor: '--input-bg',
  primaryBorderColor: '--accent',
  primaryTextColor: '--text-strong',
  textColor: '--text-body',
  lineColor: '--text',
}

function textOf(node: ElementContent): string {
  if (node.type === 'text') {
    return node.value
  }
  return node.type === 'element' ? node.children.map(textOf).join('') : ''
}

/** The Mermaid source of a `<pre>` rendered from a ```mermaid block, or null for any other code block. */
export function mermaidSourceOf(pre: Element | undefined): string | null {
  const code = pre?.children.find((child): child is Element => child.type === 'element' && child.tagName === 'code')
  const classes = code?.properties.className
  return code !== undefined && Array.isArray(classes) && classes.includes(MERMAID_CLASS) ? textOf(code) : null
}

/** The current values of the theme tokens; a token that is not defined is left to Mermaid's default. */
function themeVariablesOf(): Record<string, string> {
  const style = getComputedStyle(document.documentElement)
  const entries = Object.entries(THEME_TOKENS).map(([name, token]) => [name, style.getPropertyValue(token).trim()])
  return Object.fromEntries(entries.filter(([, value]) => value !== ''))
}

/**
 * Strict security keeps diagrams from running scripts or holding raw HTML in labels; errors are reported to the caller
 * instead of being drawn into the page.
 */
function configOf(): MermaidConfig {
  return { startOnLoad: false, securityLevel: 'strict', suppressErrorRendering: true, theme: 'base', themeVariables: themeVariablesOf() }
}

let mermaidLoad: Promise<typeof import('mermaid').default> | null = null

/** Mermaid is large, so it is loaded the first time a diagram is drawn, and set up once. A failed load is tried again next time. */
function loadedMermaid(): Promise<typeof import('mermaid').default> {
  mermaidLoad ??= import('mermaid')
    .then(({ default: mermaid }) => {
      mermaid.initialize(configOf())
      return mermaid
    })
    .catch((error: unknown) => {
      mermaidLoad = null
      throw error
    })
  return mermaidLoad
}

let diagramCount = 0

/**
 * The SVG at its natural size. Mermaid sizes a diagram as `width="100%"` capped by an inline max-width, so a wide one
 * shrinks until its text is unreadable; the cap becomes the width instead, and the page lets a wider diagram scroll.
 */
function atNaturalWidth(svg: string): string {
  const document = new DOMParser().parseFromString(svg, 'image/svg+xml')
  const root = document.documentElement
  const naturalWidth = root.style.maxWidth
  if (root.tagName !== 'svg' || naturalWidth === '') {
    return svg
  }
  root.setAttribute('width', naturalWidth)
  root.style.removeProperty('max-width')
  return new XMLSerializer().serializeToString(root)
}

/** Draws a diagram as SVG markup. It rejects when the source is not valid Mermaid. */
export async function mermaidSvgOf(source: string): Promise<string> {
  const mermaid = await loadedMermaid()
  diagramCount += 1
  const { svg } = await mermaid.render(`mermaid-diagram-${diagramCount}`, source)
  return atNaturalWidth(svg)
}
