/** Mirrors `Identity` in be/apispec.yaml: who is signed in. */
export interface Identity {
  email: string
}

/** Why a sign-in just ended without a session: the `login` query the backend redirects with (see be/apispec.yaml). */
export type LoginFailure = 'failed' | 'unauthorized'

/** Where the visitor stands: still being checked, not signed in (maybe after a failed sign-in), or signed in. */
export type SessionState =
  | { status: 'loading' }
  | { status: 'anonymous'; failure: LoginFailure | null }
  | { status: 'signedIn'; email: string }
