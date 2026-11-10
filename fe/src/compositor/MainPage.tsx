import { Contents } from '../component/Contents.tsx'
import { Footer } from '../component/Footer.tsx'
import { Header } from '../component/Header.tsx'
import { RouteContent } from '../component/RouteContent.tsx'
import { SearchNavigation } from '../component/SearchNavigation.tsx'
import { routeOf } from '../model/route.ts'
import { useListOfPages } from '../model/useListOfPages.ts'
import './MainPage.css'

/** Main page layout from fe/DESIGN.md: Header, Search and Navigation, Contents, Footer, stacked vertically. */
export function MainPage() {
  const pages = useListOfPages()
  const route = routeOf(window.location.pathname)
  return (
    <div className="main-page">
      <Header />
      <SearchNavigation pages={pages} />
      <Contents>
        <RouteContent route={route} />
      </Contents>
      <Footer />
    </div>
  )
}
