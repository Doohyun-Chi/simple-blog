/** Mirrors `Identity` in be/apispec.yaml: who is signed in. */
export interface Identity {
  email: string
}

/** Where the visitor stands: still being checked, not signed in, or signed in as an email. */
export type SessionState = { status: 'loading' } | { status: 'anonymous' } | { status: 'signedIn'; email: string }
