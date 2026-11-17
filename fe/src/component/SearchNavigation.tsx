import { googleLoginUrl } from '../model/identityApi.ts'
import type { SessionState } from '../model/Identity.ts'
import type { PageSummary } from '../model/Page.ts'
import { contentPath, searchPath } from '../model/route.ts'
import type { FormEvent } from 'react'
import './SearchNavigation.css'

interface SearchNavigationProps {
  pages: PageSummary[]
  /** Slug of the page being shown; its link is marked as the current page. */
  currentPageSlug: string | null
  /** The query of the search being shown, kept in the input; empty when not on a search. */
  currentQuery: string
  /** Who is signed in; shown at the right end of the area. */
  session: SessionState
}

/** The right end of the area: the signed-in email, or the link that starts the Google sign-in. Nothing while checking. */
function Identity({ session }: { session: SessionState }) {
  switch (session.status) {
    case 'loading':
      return null
    case 'anonymous':
      return <a className="search-nav-link search-nav-identity" href={googleLoginUrl()}>로그인</a>
    case 'signedIn':
      return <span className="search-nav-identity" title="로그인한 계정">{session.email}</span>
  }
}

/** Pressing Enter searches posts by keyword. A blank input sends nothing, as the spec requires. */
function submitSearch(event: FormEvent<HTMLFormElement>) {
  event.preventDefault()
  const query = new FormData(event.currentTarget).get('query')?.toString() ?? ''
  if (query.trim() !== '') {
    window.location.assign(searchPath(query))
  }
}

export function SearchNavigation({ pages, currentPageSlug, currentQuery, session }: SearchNavigationProps) {
  return (
    <nav className="search-nav" aria-label="Site">
      <form className="search-nav-form" role="search" onSubmit={submitSearch}>
        <input
          className="search-nav-input"
          type="search"
          name="query"
          defaultValue={currentQuery}
          placeholder="Search"
          aria-label="Search"
        />
      </form>
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
      <Identity session={session} />
    </nav>
  )
}
