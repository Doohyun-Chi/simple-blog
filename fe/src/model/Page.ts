/** Mirrors `PageSummary` in be/apispec.yaml. */
export interface PageSummary {
  id: number
  title: string
  slug: string
  parentId: number | null
  menuOrder: number
  publishedAt: string
  modifiedAt: string
}

/** Mirrors `PageList` in be/apispec.yaml. */
export interface PageList {
  pages: PageSummary[]
}
