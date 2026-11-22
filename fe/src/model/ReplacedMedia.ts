import { createContext } from 'react'

/**
 * Tells the post being written that images were replaced in the editor, by where they are loaded from: the old one
 * and the new one alike, as either may end up unused (undo brings the old one back). The image toolbar reads it from
 * context; sources that are not images of this blog are ignored.
 */
export const ReplacedMediaContext = createContext<(sources: string[]) => void>(() => {})
