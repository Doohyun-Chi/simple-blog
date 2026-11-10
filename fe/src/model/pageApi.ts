import { requestJson } from './apiClient.ts'
import type { PageList, PageSummary } from './Page.ts'

const PAGES_PATH = '/public/api/v1/pages'

/** Calls `GET /public/api/v1/pages` (operationId: listOfPages). */
export async function listOfPages(signal?: AbortSignal): Promise<PageSummary[]> {
  const body = await requestJson<PageList>(PAGES_PATH, { signal })
  return body.pages
}
