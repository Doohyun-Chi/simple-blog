import { useDocumentTitle } from '../model/useDocumentTitle.ts'

/** Sets the web page title to the site name, for a page that shows no post. Shows nothing itself. */
export function SiteTitle({ name }: { name: string }) {
  useDocumentTitle(name, null)
  return null
}
