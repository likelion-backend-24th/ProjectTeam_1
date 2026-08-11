export default function NotFound({ navigate }) {
  return (
    <div className="page">
      <p className="center-msg">페이지를 찾을 수 없습니다.</p>
      <button className="btn" onClick={() => navigate('/')}>
        홈으로 이동
      </button>
    </div>
  )
}
