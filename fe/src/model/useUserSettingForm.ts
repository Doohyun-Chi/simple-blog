import { useState } from 'react'
import { canSaveFields, fieldsOfSetting, inputOfFields, type SettingFields, settingFailureText, type UserSetting } from './UserSetting.ts'
import { logout } from './identityApi.ts'
import { renewMcpAccessKey, updateUserSetting } from './userApi.ts'

export interface UserSettingForm {
  fields: SettingFields
  edit: (changes: Partial<SettingFields>) => void
  /** The MCP access key as the server holds it, or null when none was issued yet. Read only. */
  accessKey: string | null
  busy: boolean
  failure: string | null
  canSave: boolean
  /** 변경: saves the fields, then `onSaved`. */
  save: () => Promise<void>
  /** Revoke: replaces the key once `confirmed` says yes. The server stores the new one. */
  renewKey: (confirmed: () => boolean) => Promise<void>
  /** 로그아웃: ends the session, then `onSignedOut`. */
  signOut: () => Promise<void>
}

/** The state of the personalization form, from the setting the server holds, and the calls that save it, renew the key and sign out. */
export function useUserSettingForm(setting: UserSetting, onSaved: () => void, onSignedOut: () => void): UserSettingForm {
  const [fields, updateFields] = useState(fieldsOfSetting(setting))
  const [accessKey, updateAccessKey] = useState(setting.mcpAccessKey)
  const [busy, updateBusy] = useState(false)
  const [failure, updateFailure] = useState<string | null>(null)

  async function attempt(action: () => Promise<void>) {
    updateBusy(true)
    updateFailure(null)
    try {
      await action()
    } catch (error: unknown) {
      console.error(error)
      updateFailure(settingFailureText(error))
    } finally {
      updateBusy(false)
    }
  }

  const save = () => attempt(async () => {
    await updateUserSetting(inputOfFields(fields))
    onSaved()
  })

  const renewKey = async (confirmed: () => boolean) => {
    if (confirmed()) {
      await attempt(async () => updateAccessKey((await renewMcpAccessKey()).mcpAccessKey))
    }
  }

  const signOut = () => attempt(async () => {
    await logout()
    onSignedOut()
  })

  return {
    fields,
    edit: (changes) => updateFields((current) => ({ ...current, ...changes })),
    accessKey,
    busy,
    failure,
    canSave: canSaveFields(fields) && !busy,
    save,
    renewKey,
    signOut,
  }
}
