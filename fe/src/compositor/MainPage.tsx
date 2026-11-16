import { Contents } from '../component/Contents.tsx'
import { Footer } from '../component/Footer.tsx'
import { Header } from '../component/Header.tsx'
import { SearchNavigation } from '../component/SearchNavigation.tsx'
import { StatusMessage } from '../component/StatusMessage.tsx'
import { currentPageSlug, currentQuery, type Route, routeOf } from '../model/route.ts'
import { useListOfPages } from '../model/useListOfPages.ts'
import { PostReader } from './PostReader.tsx'
import { PostSearch } from './PostSearch.tsx'
import { RecentPosts } from './RecentPosts.tsx'
import './MainPage.css'

/** Main page layout from fe/DESIGN.md: Header, Search and Navigation, Contents, Footer, stacked vertically. */
export function MainPage() {
  const pages = useListOfPages()
  const route = routeOf(window.location.pathname, window.location.search)
  return (
    <div className="main-page">
      <Header />
      <SearchNavigation pages={pages} currentPageSlug={currentPageSlug(route)} currentQuery={currentQuery(route)} />
      <Contents>
        <RouteContent route={route} />
      </Contents>
      <Footer />
    </div>
  )
}

/** What the content area shows for a route. A page and a post share the body endpoint. */
function RouteContent({ route }: { route: Route }) {
  switch (route.kind) {
    case 'home':
      return <RecentPosts />
    case 'notFound':
      return <StatusMessage>Not found: <code>{route.path}</code></StatusMessage>
    case 'search':
      return <PostSearch key={`${route.queryType}:${route.query}`} queryType={route.queryType} query={route.query} />
    case 'content':
      return <PostReader key={route.slug} slug={route.slug} />
  }
}
