import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

/**
 * Vendor code is grouped by who loads it, so that the page everybody opens stays small and the editor, which only the
 * signed-in writer opens (compositor/PostWriter.tsx), is fetched on its own. Earlier groups win.
 */
const VENDOR_GROUPS = [
  // Vite's helper that loads a lazy chunk: the page starts with it, so it must not end up in a chunk of the editor.
  { name: 'vendor-react', test: /vite[\\/]preload-helper/, priority: 100 },
  // The writer's editor, which only the writer opens, cut to chunks of at most 450 kB.
  { name: 'vendor-editor', test: /node_modules[\\/](@mdxeditor|@lexical|lexical)[\\/]/, priority: 30, maxSize: 450_000 },
  // React itself, loaded by every page.
  { name: 'vendor-react', test: /node_modules[\\/](react|react-dom|scheduler)[\\/]/, priority: 50 },
  // The Markdown pipeline, which the reader and the editor share.
  { name: 'vendor-markdown', test: /node_modules[\\/](unified|remark|rehype|micromark|mdast|hast|unist|vfile|parse5|property-information|space-separated-tokens|comma-separated-tokens|entities|decode-named-character-reference|character-entities|stringify-entities|trim-lines|trough|bail|devlop|html-url-attributes|ccount|markdown-table|zwitch|longest-streak|is-plain-obj|@ungap)/, priority: 40 },
]

// https://vite.dev/config/
// The backend endpoint is configured per mode in .env.development / .env.production (VITE_API_BASE_URL).
export default defineConfig({
  plugins: [react()],
  build: {
    rolldownOptions: {
      output: { codeSplitting: { groups: VENDOR_GROUPS } },
    },
  },
})
