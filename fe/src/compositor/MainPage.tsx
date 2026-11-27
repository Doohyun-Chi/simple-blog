import type { ReactNode } from 'react'
import { Contents } from '../component/Contents.tsx'
import { Footer } from '../component/Footer.tsx'
import { Header } from '../component/Header.tsx'
import { SiteTitle } from '../component/SiteTitle.tsx'
import { SearchNavigation } from '../component/SearchNavigation.tsx'
import { StatusMessage } from '../component/StatusMessage.tsx'
import type { SessionState } from '../model/Identity.ts'
import type { PostSummary } from '../model/Post.ts'
import { currentContentSlug, currentQuery, hasAside, type Route, routeOf, showsAsideAd } from '../model/route.ts'
import { useBlogTheme } from '../model/useBlogTheme.ts'
import { useListOfRecentPosts } from '../model/useListOfPosts.ts'
import { useSession } from '../model/useSession.ts'
import { BookShelf } from './BookShelf.tsx'
import { HomePost } from './HomePost.tsx'
import { PostList } from './PostList.tsx'
import { PostReader } from './PostReader.tsx'
import { PostReviser } from './PostReviser.tsx'
import { PostWriter } from './PostWriter.tsx'
import { UserSettingPage } from './UserSettingPage.tsx'
import { PostSearch } from './PostSearch.tsx'
import { RecentPostsAside } from './RecentPostsAside.tsx'
import type { Remote } from '../model/useRemote.ts'
import './MainPage.css'

/** What every route view may need besides the route itself. */
interface RouteContext {
  recent: Remote<PostSummary[]>
  session: SessionState
  /** The user's main title: the web page title where no post is open, and what a post's title is followed by. */
  siteName: string
}

/** Renders the content area for one kind of route, given that route. */
type RouteView<K extends Route['kind']> = (route: Extract<Route, { kind: K }>, context: RouteContext) => ReactNode

/** The content area for each kind of route; the mapped type makes sure every kind has one. A page and a post share the body endpoint. */
const ROUTE_VIEWS: { [K in Route['kind']]: RouteView<K> } = {
  home: (_route, { recent, siteName }) => <HomePost recent={recent} siteName={siteName} />,
  list: () => <PostList />,
  write: (route, { session }) => <SignInGate session={session} need="글을 쓰려면"><PostWriter starter={route.starter ?? undefined} /></SignInGate>,
  books: (_route, { session }) => <BookShelf canWrite={session.status === 'signedIn'} />,
  edit: (route, { session }) => <SignInGate session={session} need="글을 쓰려면"><PostReviser key={route.slug} slug={route.slug} /></SignInGate>,
  user: (_route, { session }) => <SignInGate session={session} need="사용자 정보를 보려면"><UserSettingPage /></SignInGate>,
  notFound: (route) => <StatusMessage>Not found: <code>{route.path}</code></StatusMessage>,
  search: (route) => <PostSearch key={`${route.queryType}:${route.query}`} queryType={route.queryType} query={route.query} />,
  content: (route, { siteName }) => <PostReader key={route.slug} slug={route.slug} siteName={siteName} />,
}

/** What the content area shows for a route. */
function RouteContent({ route, ...context }: { route: Route } & RouteContext) {
  const view = ROUTE_VIEWS[route.kind] as RouteView<Route['kind']>
  return view(route as never, context)
}

/** The info area beside the content: none for the editor, which takes the whole width. */
function asideOf(route: Route, { recent, session }: RouteContext): ReactNode {
  if (!hasAside(route)) {
    return undefined
  }
  return <RecentPostsAside recent={recent} canWrite={session.status === 'signedIn'} showAd={showsAsideAd(route)} />
}

/** Whether the web page title is just the site name: not where a post is read (home or a post), whose reader sets it. */
function showsSiteTitle(route: Route): boolean {
  return route.kind !== 'home' && route.kind !== 'content'
}

/** Main page layout from fe/DESIGN.md: Header, Search and Navigation, Contents, Footer, stacked vertically. */
export function MainPage() {
  const session = useSession()
  const recent = useListOfRecentPosts()
  const route = routeOf(window.location.pathname, window.location.search)
  const { theme } = useBlogTheme(session, route.kind === 'user')
  const context = { recent, session, siteName: theme.mainTheme }
  return (
    <div className="main-page">
      {showsSiteTitle(route) && <SiteTitle name={theme.mainTheme} />}
      <Header theme={theme} />
      <SearchNavigation
        theme={theme}
        currentSlug={currentContentSlug(route)}
        currentQuery={currentQuery(route)}
        booksShown={route.kind === 'books'}
        session={context.session}
      />
      <Contents aside={asideOf(route, context)}>
        <RouteContent route={route} {...context} />
      </Contents>
      <Footer />
    </div>
  )
}

/** Writing, revising and the user's own page need a signed-in visitor: the content opens only then, and anyone else is told to sign in. */
function SignInGate({ session, need, children }: { session: SessionState; need: string; children: ReactNode }) {
  switch (session.status) {
    case 'loading':
      return <StatusMessage>확인 중…</StatusMessage>
    case 'anonymous':
      return <StatusMessage>{need} 로그인해야 합니다.</StatusMessage>
    case 'signedIn':
      return children
  }
}
