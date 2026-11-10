import type { PageSummary } from '../model/Page.ts'
import { contentPath } from '../model/route.ts'
import './SearchNavigation.css'

interface SearchNavigationProps {
  pages: PageSummary[]
  /** Slug of the page being shown; its link is marked as the current page. */
  currentPageSlug: string | null
}

export function SearchNavigation({ pages, currentPageSlug }: SearchNavigationProps) {
  return (
    <nav className="search-nav" aria-label="Site">
      <input className="search-nav-input" type="search" placeholder="Search" aria-label="Search" />
      <ul className="search-nav-pages">
        {pages.map((page) => (
          <li key={page.id}>
            <a
              className="search-nav-link"
              href={contentPath('page', page.slug)}
              page-current={page.slug === currentPageSlug ? 'selected' : undefined}
            >
              {page.title}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  )
}
