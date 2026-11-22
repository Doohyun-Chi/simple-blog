import { PostType } from '../model/Post.ts'
import { contentPath } from '../model/route.ts'
import './Header.css'

/** Slug of the page the subtitle links to (fe/DESIGN.md). */
const ABOUT_SLUG = 'about'

/** The title links to home and the subtitle to the about page. Both keep the plain text look; see Header.css. */
export function Header() {
  return (
    <header className="header">
      <h1 className="header-title">
        <a className="header-link" href="/">Dreaming for the Future</a>
      </h1>
      <p className="header-subtitle">
        <a className="header-link" href={contentPath(PostType.PAGE, ABOUT_SLUG)}>영원한 개발자를 향해</a>
      </p>
    </header>
  )
}
