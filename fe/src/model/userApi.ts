import { requestJson } from './apiClient.ts'
import { csrfHeaders } from './csrfToken.ts'
import type { McpAccessKey, UserSetting, UserSettingInput } from './UserSetting.ts'

const USER_PATH = '/public/api/v1/user'

/** Calls `GET /public/api/v1/user` (operationId: userSetting) with the session cookie. */
export function userSetting(signal?: AbortSignal): Promise<UserSetting> {
  return requestJson<UserSetting>(USER_PATH, { signal, credentials: 'include' })
}

/** Calls `PUT /public/api/v1/user` (operationId: updateUserSetting): saves what the user may change and answers the saved setting. */
export function updateUserSetting(input: UserSettingInput, signal?: AbortSignal): Promise<UserSetting> {
  const headers = { 'Content-Type': 'application/json', ...csrfHeaders() }
  return requestJson<UserSetting>(USER_PATH, { method: 'PUT', headers, body: JSON.stringify(input), credentials: 'include', signal })
}

/** Calls `PUT /public/api/v1/user/access-token` (operationId: renewMcpAccessKey): the old key stops working; the server keeps the new one. */
export function renewMcpAccessKey(signal?: AbortSignal): Promise<McpAccessKey> {
  return requestJson<McpAccessKey>(`${USER_PATH}/access-token`, { method: 'PUT', headers: csrfHeaders(), credentials: 'include', signal })
}
