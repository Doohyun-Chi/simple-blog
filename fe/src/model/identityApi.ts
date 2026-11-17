import { ApiError, apiUrl, requestJson } from './apiClient.ts'
import type { Identity } from './Identity.ts'

const ME_PATH = '/public/api/v1/me'
const GOOGLE_LOGIN_PATH = '/public/auth/google'

/**
 * Calls `GET /public/api/v1/me` (operationId: me) with the session cookie. Resolves to null when nobody is signed in
 * (401), so that is an ordinary answer and not an error.
 */
export async function identity(signal?: AbortSignal): Promise<Identity | null> {
  try {
    return await requestJson<Identity>(ME_PATH, { signal, credentials: 'include' })
  } catch (error: unknown) {
    if (error instanceof ApiError && error.status === 401) {
      return null
    }
    throw error
  }
}

/**
 * Where the 로그인 link goes: the backend's `/public/auth/google` (operationId: startGoogleLogin). The backend runs the
 * whole Google sign-in and sends the browser back here, so the frontend holds no OAuth code, token or secret.
 */
export function googleLoginUrl(): string {
  return apiUrl(GOOGLE_LOGIN_PATH)
}
