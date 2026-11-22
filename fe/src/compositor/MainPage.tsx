import type { ReactNode } from 'react'
import { Contents } from '../component/Contents.tsx'
import { Footer } from '../component/Footer.tsx'
import { Header } from '../component/Header.tsx'
import { SearchNavigation } from '../component/SearchNavigation.tsx'
import { StatusMessage } from '../component/StatusMessage.tsx'
import type { SessionState } from '../model/Identity.ts'
import type { PostSummary } from '../model/Post.ts'
import { currentPageSlug, currentQuery, isEditorRoute, type Route, routeOf, showsAsideAd } from '../model/route.ts'
import { useListOfPages } from '../model/useListOfPages.ts'
import { useListOfRecentPosts } from '../model/useListOfPosts.ts'
import { useSession } from '../model/useSession.ts'
import { HomePost } from './HomePost.tsx'
import { PostList } from './PostList.tsx'
import { PostReader } from './PostReader.tsx'
import { PostReviser } from './PostReviser.tsx'
import { PostWriter } from './PostWriter.tsx'
import { PostSearch } from './PostSearch.tsx'
import { RecentPostsAside } from './RecentPostsAside.tsx'
import type { Remote } from '../model/useRemote.ts'
import './MainPage.css'

/** What every route view may need besides the route itself. */
interface RouteContext {
  recent: Remote<PostSummary[]>
  session: SessionState
}

/** Renders the content area for one kind of route, given that route. */
type RouteView<K extends Route['kind']> = (route: Extract<Route, { kind: K }>, context: RouteContext) => ReactNode

/** The content area for each kind of route; the mapped type makes sure every kind has one. A page and a post share the body endpoint. */
const ROUTE_VIEWS: { [K in Route['kind']]: RouteView<K> } = {
  home: (_route, { recent }) => <HomePost recent={recent} />,
  list: () => <PostList />,
  write: (_route, { session }) => <WriteGate session={session}><PostWriter /></WriteGate>,
  edit: (route, { session }) => <WriteGate session={session}><PostReviser key={route.slug} slug={route.slug} /></WriteGate>,
  notFound: (route) => <StatusMessage>Not found: <code>{route.path}</code></StatusMessage>,
  search: (route) => <PostSearch key={`${route.queryType}:${route.query}`} queryType={route.queryType} query={route.query} />,
  content: (route) => <PostReader key={route.slug} slug={route.slug} />,
}

/** What the content area shows for a route. */
function RouteContent({ route, ...context }: { route: Route } & RouteContext) {
  const view = ROUTE_VIEWS[route.kind] as RouteView<Route['kind']>
  return view(route as never, context)
}

/** The info area beside the content: none for the editor, which takes the whole width. */
function asideOf(route: Route, { recent, session }: RouteContext): ReactNode {
  if (isEditorRoute(route)) {
    return undefined
  }
  return <RecentPostsAside recent={recent} canWrite={session.status === 'signedIn'} showAd={showsAsideAd(route)} />
}

/** Main page layout from fe/DESIGN.md: Header, Search and Navigation, Contents, Footer, stacked vertically. */
export function MainPage() {
  const pages = useListOfPages()
  const context = { recent: useListOfRecentPosts(), session: useSession() }
  const route = routeOf(window.location.pathname, window.location.search)
  return (
    <div className="main-page">
      <Header />
      <SearchNavigation
        pages={pages}
        currentPageSlug={currentPageSlug(route)}
        currentQuery={currentQuery(route)}
        session={context.session}
      />
      <Contents aside={asideOf(route, context)}>
        <RouteContent route={route} {...context} />
      </Contents>
      <Footer />
    </div>
  )
}

/** Writing and revising need a signed-in visitor: the editor opens only then, and anyone else is told to sign in. */
function WriteGate({ session, children }: { session: SessionState; children: ReactNode }) {
  switch (session.status) {
    case 'loading':
      return <StatusMessage>확인 중…</StatusMessage>
    case 'anonymous':
      return <StatusMessage>글을 쓰려면 로그인해야 합니다.</StatusMessage>
    case 'signedIn':
      return children
  }
}
