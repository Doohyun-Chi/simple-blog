import { IconBooks, IconLogin, IconMenu2 } from '@tabler/icons-react'
import { Button, Icon, TextInput } from 'design-system'
import { type FormEvent, useState } from 'react'
import { googleLoginUrl, returnPathOf } from '../model/identityApi.ts'
import type { LoginFailure, SessionState } from '../model/Identity.ts'
import { PostType } from '../model/Post.ts'
import { BOOKS_PATH, contentPath, searchPath, USER_PATH } from '../model/route.ts'
import type { BlogTheme } from '../model/UserSetting.ts'
import './SearchNavigation.css'

interface SearchNavigationProps {
  /** The about and working links the user personalizes. */
  theme: BlogTheme
  /** Slug of the post or page being shown; the navigation link to it is marked as the current page. */
  currentSlug: string | null
  /** The query of the search being shown, kept in the input; empty when not on a search. */
  currentQuery: string
  /** Whether the bookshelf is the content shown; its link is marked as the current page. */
  booksShown: boolean
  /** Who is signed in; shown at the right end of the area. */
  session: SessionState
}

const LOGIN_FAILURE_MESSAGES: Record<LoginFailure, string> = {
  failed: '로그인하지 못했습니다.',
  unauthorized: '허용되지 않은 사용자입니다. (Unauthorized user)',
}

/** The user's item: the sign-in link, or the signed-in email, which opens the personalization page. Nothing while checking. */
function Identity({ session }: { session: SessionState }) {
  switch (session.status) {
    case 'loading':
      return null
    case 'anonymous':
      return (
        <>
          {session.failure && <span className="search-nav-notice" role="alert">{LOGIN_FAILURE_MESSAGES[session.failure]}</span>}
          <a className="search-nav-link search-nav-identity" href={googleLoginUrl()}
            onClick={(event) => { event.currentTarget.href = googleLoginUrl(returnPathOf(window.location)) }}>
            <Icon icon={IconLogin} />로그인
          </a>
        </>
      )
    case 'signedIn':
      return <a className="search-nav-link search-nav-identity" href={USER_PATH} title="내 정보">{session.email}</a>
  }
}

/** A navigation link to a post or page, marked when it is the one being shown. */
function PostLink({ name, slug, currentSlug }: { name: string; slug: string; currentSlug: string | null }) {
  return (
    <a className="search-nav-link search-nav-item" href={contentPath(PostType.POST, slug)} page-current={slug === currentSlug ? 'selected' : undefined}>
      {name}
    </a>
  )
}

/** Pressing Enter searches posts by keyword. A blank input sends nothing, as the spec requires. */
function submitSearch(event: FormEvent<HTMLFormElement>) {
  event.preventDefault()
  const query = new FormData(event.currentTarget).get('query')?.toString() ?? ''
  if (query.trim() !== '') {
    window.location.assign(searchPath(query))
  }
}

/**
 * The search box and the navigation items: the about and working links, the bookshelf and the user. On a narrow screen the
 * items are behind a More button (see SearchNavigation.css).
 */
export function SearchNavigation({ theme, currentSlug, currentQuery, booksShown, session }: SearchNavigationProps) {
  const [open, updateOpen] = useState(false)
  return (
    <nav className="search-nav" aria-label="Site">
      <form className="search-nav-form" role="search" onSubmit={submitSearch}>
        <TextInput size="sm" className="search-nav-input" type="search" name="query" defaultValue={currentQuery} placeholder="Search" aria-label="Search" />
      </form>
      <Button variant="ghost" size="sm" icon={IconMenu2} className="search-nav-more" aria-expanded={open} aria-controls="search-nav-items"
        onClick={() => updateOpen(!open)}>More</Button>
      <div className="search-nav-items" id="search-nav-items" data-open={open}>
        <PostLink name={theme.aboutName} slug={theme.aboutSlug} currentSlug={currentSlug} />
        <PostLink name={theme.workingName} slug={theme.workingSlug} currentSlug={currentSlug} />
        <a className="search-nav-link search-nav-item" href={BOOKS_PATH} page-current={booksShown ? 'selected' : undefined}>
          <Icon icon={IconBooks} />내서재
        </a>
        <Identity session={session} />
      </div>
    </nav>
  )
}
