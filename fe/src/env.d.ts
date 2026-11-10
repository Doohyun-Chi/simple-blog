/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Backend origin, e.g. `http://localhost:8080`. Empty means same origin as the FE. */
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
