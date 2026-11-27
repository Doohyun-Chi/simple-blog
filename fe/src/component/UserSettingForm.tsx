import { IconCheck, IconCopy, IconLogout } from '@tabler/icons-react'
import { PostType } from '../model/Post.ts'
import { contentPath } from '../model/route.ts'
import {
  cleanSettingSlug, LINK_NAME_MAX_LENGTH, NICK_MAX_LENGTH, SETTING_SLUG_MAX_LENGTH, THEME_MAX_LENGTH, type UserSetting,
} from '../model/UserSetting.ts'
import { useCopyToClipboard } from '../model/useCopyToClipboard.ts'
import { useUserSettingForm } from '../model/useUserSettingForm.ts'
import './UserSettingForm.css'

const REVOKE_QUESTION = 'MCP 조회키를 새로 발급할까요? 지금 키는 바로 쓸 수 없게 됩니다.'

interface UserSettingFormProps {
  setting: UserSetting
  /** Called after the changes are saved. */
  onSaved: () => void
  /** 돌아가기: leaves without saving. */
  onBack: () => void
  /** Called after 로그아웃 ended the session. */
  onSignedOut: () => void
}

interface TextFieldProps {
  name: string
  label: string
  value: string
  maxLength: number
  hint?: string
  onEdit: (value: string) => void
}

function TextField({ name, label, value, maxLength, hint, onEdit }: TextFieldProps) {
  return (
    <label className="user-setting-field">
      <span className="user-setting-label">{label}</span>
      <input className="user-setting-input" name={name} type="text" value={value} maxLength={maxLength} onChange={(event) => onEdit(event.target.value)} />
      {hint !== undefined && <span className="user-setting-hint">{hint}</span>}
    </label>
  )
}

interface LinkFieldsProps {
  name: string
  label: string
  text: string
  slug: string
  onText: (text: string) => void
  onSlug: (slug: string) => void
}

/** The text of a navigation link and the slug of the post it opens, on one line; the post's path is shown under them. */
function LinkFields({ name, label, text, slug, onText, onSlug }: LinkFieldsProps) {
  return (
    <div className="user-setting-field">
      <span className="user-setting-label">{label}</span>
      <div className="user-setting-link">
        <input className="user-setting-input" name={`${name}-name`} type="text" aria-label={`${label} 표시 문구`} placeholder="표시 문구"
          value={text} maxLength={LINK_NAME_MAX_LENGTH} onChange={(event) => onText(event.target.value)} />
        <input className="user-setting-input" name={`${name}-slug`} type="text" aria-label={`${label} slug`} placeholder="slug"
          value={slug} maxLength={SETTING_SLUG_MAX_LENGTH} onChange={(event) => onSlug(event.target.value)} />
      </div>
      <span className="user-setting-hint">{contentPath(PostType.POST, slug)}</span>
    </div>
  )
}

/** The MCP access key, read only, with copying it and replacing it (Revoke). The replaced key is kept by the server. */
function AccessKeyField({ accessKey, busy, onRevoke }: { accessKey: string | null; busy: boolean; onRevoke: () => void }) {
  const { copied, copy } = useCopyToClipboard(accessKey ?? '')
  return (
    <div className="user-setting-field">
      <span className="user-setting-label">MCP 조회키</span>
      <div className="user-setting-key">
        <input className="user-setting-input" name="mcp-access-key" type="text" readOnly value={accessKey ?? ''} placeholder="아직 발급되지 않았습니다." />
        <button type="button" disabled={accessKey === null} onClick={copy}>
          {copied ? <IconCheck size="1.1em" aria-hidden="true" /> : <IconCopy size="1.1em" aria-hidden="true" />}
          {copied ? '복사됨' : '복사'}
        </button>
        <button type="button" disabled={busy} onClick={onRevoke}>{accessKey === null ? '발급' : 'Revoke'}</button>
      </div>
    </div>
  )
}

/** The personalization of the signed-in user: nick name, the two titles, the text and page of the about and working links, and the MCP key. */
export function UserSettingForm({ setting, onSaved, onBack, onSignedOut }: UserSettingFormProps) {
  const form = useUserSettingForm(setting, onSaved, onSignedOut)
  const { fields, edit } = form
  return (
    <form className="user-setting" aria-label="개인화" onSubmit={(event) => event.preventDefault()}>
      <h2 className="user-setting-title">개인화</h2>
      <TextField name="nick" label="닉네임" value={fields.nick} maxLength={NICK_MAX_LENGTH} onEdit={(nick) => edit({ nick })} />
      <TextField name="main-theme" label="표제어" value={fields.mainTheme} maxLength={THEME_MAX_LENGTH} onEdit={(mainTheme) => edit({ mainTheme })} />
      <TextField name="sub-theme" label="부제어" value={fields.subTheme} maxLength={THEME_MAX_LENGTH} onEdit={(subTheme) => edit({ subTheme })} />
      <LinkFields name="about" label="개인 설명 페이지 (about)" text={fields.aboutName} slug={fields.aboutSlug}
        onText={(aboutName) => edit({ aboutName })} onSlug={(aboutSlug) => edit({ aboutSlug: cleanSettingSlug(aboutSlug) })} />
      <LinkFields name="working" label="이력 설명 페이지 (working)" text={fields.workingName} slug={fields.workingSlug}
        onText={(workingName) => edit({ workingName })} onSlug={(workingSlug) => edit({ workingSlug: cleanSettingSlug(workingSlug) })} />
      <AccessKeyField accessKey={form.accessKey} busy={form.busy} onRevoke={() => void form.renewKey(() => window.confirm(REVOKE_QUESTION))} />
      {form.failure !== null && <p className="user-setting-failure" role="alert">{form.failure}</p>}
      <div className="user-setting-actions">
        <button type="button" className="user-setting-sign-out" disabled={form.busy} onClick={() => void form.signOut()}>
          <IconLogout size="1.1em" aria-hidden="true" />로그아웃
        </button>
        <button type="button" disabled={!form.canSave} onClick={() => void form.save()}>변경</button>
        <button type="button" onClick={onBack}>돌아가기</button>
      </div>
    </form>
  )
}
