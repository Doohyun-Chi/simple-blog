import { Contents } from '../component/Contents.tsx'
import { Footer } from '../component/Footer.tsx'
import { Header } from '../component/Header.tsx'
import { SearchNavigation } from '../component/SearchNavigation.tsx'
import { StatusMessage } from '../component/StatusMessage.tsx'
import type { PostSummary } from '../model/Post.ts'
import { currentPageSlug, currentQuery, type Route, routeOf } from '../model/route.ts'
import { useListOfPages } from '../model/useListOfPages.ts'
import { useListOfRecentPosts } from '../model/useListOfPosts.ts'
import { HomePost } from './HomePost.tsx'
import { PostList } from './PostList.tsx'
import { PostReader } from './PostReader.tsx'
import { PostWriter } from './PostWriter.tsx'
import { PostSearch } from './PostSearch.tsx'
import { RecentPostsAside } from './RecentPostsAside.tsx'
import type { Remote } from '../model/useRemote.ts'
import './MainPage.css'

/** Main page layout from fe/DESIGN.md: Header, Search and Navigation, Contents, Footer, stacked vertically. */
export function MainPage() {
  const pages = useListOfPages()
  const recent = useListOfRecentPosts()
  const route = routeOf(window.location.pathname, window.location.search)
  return (
    <div className="main-page">
      <Header />
      <SearchNavigation pages={pages} currentPageSlug={currentPageSlug(route)} currentQuery={currentQuery(route)} />
      <Contents aside={route.kind === 'write' ? undefined : <RecentPostsAside recent={recent} />}>
        <RouteContent route={route} recent={recent} />
      </Contents>
      <Footer />
    </div>
  )
}

/** What the content area shows for a route. A page and a post share the body endpoint. */
function RouteContent({ route, recent }: { route: Route; recent: Remote<PostSummary[]> }) {
  switch (route.kind) {
    case 'home':
      return <HomePost recent={recent} />
    case 'list':
      return <PostList />
    case 'write':
      return <PostWriter />
    case 'notFound':
      return <StatusMessage>Not found: <code>{route.path}</code></StatusMessage>
    case 'search':
      return <PostSearch key={`${route.queryType}:${route.query}`} queryType={route.queryType} query={route.query} />
    case 'content':
      return <PostReader key={route.slug} slug={route.slug} />
  }
}
