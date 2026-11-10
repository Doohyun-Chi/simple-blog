import type { Route } from '../model/route.ts'
import './RouteContent.css'

interface RouteContentProps {
  route: Route
}

/** What the content area shows for a route. Until the BE is wired up, a page or post shows only its type and slug. */
export function RouteContent({ route }: RouteContentProps) {
  switch (route.kind) {
    case 'home':
      return null
    case 'notFound':
      return <p className="route-content-not-found">Not found: <code>{route.path}</code></p>
    case 'content':
      return (
        <dl className="route-content">
          <dt>Type</dt>
          <dd>{route.type}</dd>
          <dt>Slug</dt>
          <dd><code>{route.slug}</code></dd>
        </dl>
      )
  }
}
