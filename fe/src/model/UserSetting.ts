import { ApiError } from './apiClient.ts'
import { cleanSlug } from './Post.ts'

/** Mirrors `UserSetting` in be/apispec.yaml. `modifiedDate` is null while the user has saved nothing, so the rest is the defaults. */
export interface UserSetting {
  email: string
  nick: string | null
  mainTheme: string
  subTheme: string
  aboutName: string
  aboutSlug: string
  workingName: string
  workingSlug: string
  mcpAccessKey: string | null
  modifiedDate: string | null
}

/** Mirrors `UserSettingInput`: what the user may change. */
export interface UserSettingInput {
  nick: string | null
  mainTheme: string
  subTheme: string
  aboutName: string
  aboutSlug: string
  workingName: string
  workingSlug: string
}

/** Mirrors `McpAccessKey`. */
export interface McpAccessKey {
  mcpAccessKey: string
}

/** What the header and the navigation show: the titles and the two links the user personalizes. */
export interface BlogTheme {
  mainTheme: string
  subTheme: string
  aboutName: string
  aboutSlug: string
  workingName: string
  workingSlug: string
}

/** The theme of a visitor who is not signed in, and of the user until it is saved (fe/DESIGN.md, Navigation). Same as the backend's defaults. */
export const DEFAULT_THEME: BlogTheme = {
  mainTheme: 'Dreaming for the Future',
  subTheme: '영원한 개발자를 향해',
  aboutName: '나의 꿈',
  aboutSlug: 'about',
  workingName: '제가 일하는 곳은?',
  workingSlug: 'my-working-places',
}

/** The limits of `UserSettingInput` in be/apispec.yaml. */
export const NICK_MAX_LENGTH = 60
export const THEME_MAX_LENGTH = 256
export const LINK_NAME_MAX_LENGTH = 100
export const SETTING_SLUG_MAX_LENGTH = 200

/** The fields of the personalization form, as typed. */
export interface SettingFields {
  nick: string
  mainTheme: string
  subTheme: string
  aboutName: string
  aboutSlug: string
  workingName: string
  workingSlug: string
}

export function fieldsOfSetting(setting: UserSetting): SettingFields {
  return {
    nick: setting.nick ?? '',
    mainTheme: setting.mainTheme,
    subTheme: setting.subTheme,
    aboutName: setting.aboutName,
    aboutSlug: setting.aboutSlug,
    workingName: setting.workingName,
    workingSlug: setting.workingSlug,
  }
}

/** The request for what was typed: a blank nick is sent as null. */
export function inputOfFields(fields: SettingFields): UserSettingInput {
  return { ...fields, nick: fields.nick.trim() === '' ? null : fields.nick.trim() }
}

/** Turns typed text into slug characters, as for a post. */
export function cleanSettingSlug(text: string): string {
  return cleanSlug(text).slice(0, SETTING_SLUG_MAX_LENGTH)
}

/** Whether the form can be sent: a main theme, and a text and a slug for both links, which the backend refuses empty. */
export function canSaveFields(fields: SettingFields): boolean {
  const required = [fields.mainTheme, fields.aboutName, fields.aboutSlug, fields.workingName, fields.workingSlug]
  return required.every((text) => text.trim() !== '')
}

/** Whether this is a user who has not saved anything yet, which sends them to the personalization page once. */
export function isFirstVisit(setting: UserSetting): boolean {
  return setting.modifiedDate === null
}

/** The theme part of the personalization. */
export function themeOf(setting: UserSetting): BlogTheme {
  return {
    mainTheme: setting.mainTheme,
    subTheme: setting.subTheme,
    aboutName: setting.aboutName,
    aboutSlug: setting.aboutSlug,
    workingName: setting.workingName,
    workingSlug: setting.workingSlug,
  }
}

/** What to tell the user when the personalization could not be saved or the key renewed, by what the backend answered. */
export function settingFailureText(error: unknown): string {
  switch (error instanceof ApiError ? error.status : 0) {
    case 400:
      return '입력한 내용을 확인해 주세요. slug는 영문, 숫자, -만 쓸 수 있습니다.'
    case 401:
    case 403:
      return '권한이 없습니다. 다시 로그인해 주세요.'
    default:
      return '저장하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  }
}
