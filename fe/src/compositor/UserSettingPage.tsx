import { StatusMessage } from '../component/StatusMessage.tsx'
import { UserSettingForm } from '../component/UserSettingForm.tsx'
import { returnPathOf } from '../model/route.ts'
import { userSetting } from '../model/userApi.ts'
import { useRemote } from '../model/useRemote.ts'

/** Goes back to the page the visitor came from, loading it afresh so the header shows the saved titles. */
function leave() {
  window.location.assign(returnPathOf(document.referrer, window.location.origin))
}

/** After signing out the visitor starts over at home. */
function leaveSignedOut() {
  window.location.assign('/')
}

/** The personalization page: loads what the server holds for the signed-in user and shows it in the form. */
export function UserSettingPage() {
  const setting = useRemote('user-setting', (signal) => userSetting(signal))
  switch (setting.state) {
    case 'loading':
      return <StatusMessage>불러오는 중…</StatusMessage>
    case 'failed':
      return <StatusMessage>사용자 정보를 불러오지 못했습니다.</StatusMessage>
    case 'loaded':
      return <UserSettingForm setting={setting.value} onSaved={leave} onBack={leave} onSignedOut={leaveSignedOut} />
  }
}
