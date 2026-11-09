import type { PageList, PageSummary } from './Page.ts'

const PAGES_URL = '/public/api/v1/pages'

/** Calls `GET /public/api/v1/pages` (operationId: listOfPages). */
export async function listOfPages(signal?: AbortSignal): Promise<PageSummary[]> {
  const response = await fetch(PAGES_URL, { signal })
  if (!response.ok) {
    throw new Error(`GET ${PAGES_URL} failed: ${response.status}`)
  }
  const body = (await response.json()) as PageList
  return body.pages
}

export function topLevelPages(pages: PageSummary[]): PageSummary[] {
  return pages.filter((page) => page.parentId === null)
}
