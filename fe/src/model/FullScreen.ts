import { createContext } from 'react'

/** Whether the editor fills the browser area, and how to flip that. The editor's toolbar reads it from context. */
export interface FullScreen {
  on: boolean
  toggle: () => void
}

export const FullScreenContext = createContext<FullScreen>({ on: false, toggle: () => {} })
