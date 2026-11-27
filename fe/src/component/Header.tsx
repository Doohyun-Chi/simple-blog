import { PostType } from '../model/Post.ts'
import { contentPath } from '../model/route.ts'
import type { BlogTheme } from '../model/UserSetting.ts'
import './Header.css'

/** The main title links to home and the sub title to the about page (fe/DESIGN.md). Both keep the plain text look; see Header.css. */
export function Header({ theme }: { theme: BlogTheme }) {
  return (
    <header className="header">
      <h1 className="header-title">
        <a className="header-link" href="/">{theme.mainTheme}</a>
      </h1>
      {theme.subTheme !== '' && (
        <p className="header-subtitle">
          <a className="header-link" href={contentPath(PostType.POST, theme.aboutSlug)}>{theme.subTheme}</a>
        </p>
      )}
    </header>
  )
}
