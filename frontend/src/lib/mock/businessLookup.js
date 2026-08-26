// TODO: 팀원이 실제 사업자 조회(국세청 연동) API를 만들면
// 아래 로직을 실제 fetch 호출로 교체하면 됩니다.
// 지금은 형식이 맞으면 더미 데이터를 돌려주는 임시(가짜) 로직이에요.
export async function mockBusinessLookup(businessRegistrationNumber) {
  // 실제 API 호출처럼 살짝 딜레이를 줍니다.
  await new Promise((resolve) => setTimeout(resolve, 700));

  const isValidFormat = /^\d{3}-\d{2}-\d{5}$/.test(businessRegistrationNumber);
  if (!isValidFormat) {
    return { matched: false };
  }

  // TODO: 실제 API 연동 시 이 더미 데이터 대신 응답값을 그대로 사용
  return {
    matched: true,
    businessName: "청년팜 협동조합",
    representativeName: "김도시",
    businessType: "농업, 밭 임대업",
  };
}