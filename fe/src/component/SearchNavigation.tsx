import type { PageSummary } from '../model/Page.ts'
import './SearchNavigation.css'

interface SearchNavigationProps {
  pages: PageSummary[]
}

export function SearchNavigation({ pages }: SearchNavigationProps) {
  return (
    <nav className="search-nav" aria-label="Site">
      <input className="search-nav-input" type="search" placeholder="Search" aria-label="Search" />
      <ul className="search-nav-pages">
        {pages.map((page) => (
          <li key={page.id}>
            <a className="search-nav-link" href={`/${page.slug}`}>
              {page.title}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  )
}
