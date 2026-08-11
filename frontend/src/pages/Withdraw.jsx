import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useHashLocation } from '../router'
import TopBar from '../components/TopBar'

export default function Withdraw() {
  const { user } = useAuth()
  const [, navigate] = useHashLocation()
  const [checked, setChecked] = useState(false)
  const [notice, setNotice] = useState('')

  const handleWithdraw = () => {
    // 백엔드에 회원 본인 탈퇴 API가 아직 없어 실제 탈퇴 처리는 불가능합니다.
    // (AdminController의 상태 변경 API는 ADMIN 권한이 있어야 호출할 수 있습니다.)
    setNotice('현재 회원 탈퇴 API가 백엔드에 준비되어 있지 않습니다. 관리자에게 문의해주세요.')
  }

  return (
    <>
      <TopBar title="회원탈퇴" backTo="/profile" />

      <div className="page no-pad-bottom">
        <h1 className="title-lg">{user?.nickName}님</h1>
        <p className="subtitle">정말 탈퇴하시겠어요?</p>

        <div className="notice-box">
          <ul>
            <li>타인 글의 댓글은 삭제되지 않으니 미리 확인해주세요.</li>
            <li>게시된 글은 삭제되지 않으니 미리 확인해주세요.</li>
            <li>탈퇴 후 동일한 이메일로 재가입이 제한될 수 있습니다.</li>
          </ul>
        </div>

        <label className="checkbox-row">
          <input type="checkbox" checked={checked} onChange={(e) => setChecked(e.target.checked)} />
          안내사항을 모두 확인했습니다.
        </label>

        {notice && <p className="hint-text">{notice}</p>}

        <button className="btn danger" disabled={!checked} onClick={handleWithdraw}>
          탈퇴하기
        </button>
      </div>
    </>
  )
}
