import type { LoginFailure, SessionState } from './Identity.ts'
import { identity } from './identityApi.ts'
import { useRemote } from './useRemote.ts'

/** The failure the backend reported through the `login` query after a sign-in, or null when there is none. */
export function loginFailureOf(search: string): LoginFailure | null {
  const login = new URLSearchParams(search).get('login')
  return login === 'failed' || login === 'unauthorized' ? login : null
}

/**
 * Asks the backend once who is signed in. If the backend cannot be reached the visitor is treated as signed out, so
 * the 로그인 link still shows and the rest of the page is unaffected.
 */
export function useSession(): SessionState {
  const who = useRemote('session', (signal) => identity(signal))
  if (who.state === 'loading') {
    return { status: 'loading' }
  }
  if (who.state === 'loaded' && who.value !== null) {
    return { status: 'signedIn', email: who.value.email }
  }
  return { status: 'anonymous', failure: loginFailureOf(window.location.search) }
}
