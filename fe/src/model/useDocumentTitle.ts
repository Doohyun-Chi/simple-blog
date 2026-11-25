import { useEffect } from 'react'

/** The name of the site: the title of the web page where no post is open. */
export const SITE_NAME = 'Dreaming for the Future'

/** The web page title of a post or page: `{title} | Dreaming for the Future`. */
export function documentTitleOf(postTitle: string): string {
  return `${postTitle} | ${SITE_NAME}`
}

/** Shows the post's title in the web page title while it is open, then puts the site name back. Null (not loaded yet) leaves the title alone. */
export function useDocumentTitle(postTitle: string | null): void {
  useEffect(() => {
    if (postTitle === null) {
      return
    }
    document.title = documentTitleOf(postTitle)
    return () => {
      document.title = SITE_NAME
    }
  }, [postTitle])
}
