import { useEffect } from 'react'

/** The web page title: `{post title} | {site name}` while a post is open, otherwise the site name, the user's main title. */
export function documentTitleOf(siteName: string, postTitle: string | null): string {
  return postTitle === null ? siteName : `${postTitle} | ${siteName}`
}

/** Shows the title in the web page title. A null post title (none open, or not loaded yet) shows the site name alone. */
export function useDocumentTitle(siteName: string, postTitle: string | null): void {
  useEffect(() => {
    document.title = documentTitleOf(siteName, postTitle)
  }, [siteName, postTitle])
}
