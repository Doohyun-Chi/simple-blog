import { useEffect } from 'react'

/**
 * While `unsaved` is true, closing, reloading or leaving the page makes the browser ask whether to leave and lose
 * the unsaved work. The browser words the question itself; a page cannot change it.
 */
export function useLeaveGuard(unsaved: boolean): void {
  useEffect(() => {
    if (!unsaved) {
      return
    }
    const ask = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }
    window.addEventListener('beforeunload', ask)
    return () => window.removeEventListener('beforeunload', ask)
  }, [unsaved])
}
