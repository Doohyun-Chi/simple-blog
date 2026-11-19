/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Backend origin, e.g. `http://localhost:8080`. Empty means same origin as the FE. */
  readonly VITE_API_BASE_URL?: string
  /** Google Analytics 4 measurement ID (`G-…`). Empty means analytics is off. */
  readonly VITE_GA_MEASUREMENT_ID?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
