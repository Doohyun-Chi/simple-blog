import { useEffect } from 'react'
import type { SessionState } from './Identity.ts'
import { USER_PATH } from './route.ts'
import { DEFAULT_THEME, type BlogTheme, isFirstVisit, themeOf, type UserSetting } from './UserSetting.ts'
import { userSetting } from './userApi.ts'
import { useRemote } from './useRemote.ts'

/** Set once the visitor has been sent to the personalization page, so a first visit sends them there only once per tab. */
const SENT_KEY = 'personalization-offered'

export interface BlogThemeState {
  theme: BlogTheme
  /** Signed in with nothing saved yet. */
  firstVisit: boolean
}

function remembered(): boolean {
  try {
    return sessionStorage.getItem(SENT_KEY) !== null
  } catch {
    return true
  }
}

function remember() {
  try {
    sessionStorage.setItem(SENT_KEY, 'yes')
  } catch {
    // Without storage the visitor is simply not sent.
  }
}

/**
 * The theme to show: the signed-in user's own, or the defaults for a visitor and while it loads (fe/DESIGN.md, Navigation).
 * A signed-in user who has saved nothing is sent to the personalization page once, unless already there.
 */
export function useBlogTheme(session: SessionState, onUserPage: boolean): BlogThemeState {
  const email = session.status === 'signedIn' ? session.email : null
  const setting = useRemote<UserSetting | null>(`user:${email ?? ''}`, (signal) => (email === null ? Promise.resolve(null) : userSetting(signal)))
  const loaded = setting.state === 'loaded' ? setting.value : null
  const firstVisit = loaded !== null && isFirstVisit(loaded)
  useEffect(() => {
    if (firstVisit && !onUserPage && !remembered()) {
      remember()
      window.location.assign(USER_PATH)
    }
  }, [firstVisit, onUserPage])
  return { theme: loaded === null ? DEFAULT_THEME : themeOf(loaded), firstVisit }
}
