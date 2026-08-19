// PortOne 결제/빌링키 연동을 위한 유일한 진입점.
//
// 지켜야 할 원칙 (요구사항 14절):
// - 이 파일 밖에서는 절대 PortOne SDK를 직접 호출하지 않는다.
// - 여기서 다루는 storeId/channelKey는 공개 식별자이며 NEXT_PUBLIC_* 환경변수로만 관리한다.
// - API Secret 등 서버 전용 키는 이 프로젝트(프론트) 어디에도 절대 두지 않는다.
// - 이 모듈이 반환하는 결과는 "결제창 호출 결과"일 뿐 최종 결제 성공/실패가 아니다.
//   반드시 백엔드 검증(services/payment.verifyPayment 등)을 거친 뒤에만 화면에 최종 상태를 표시한다.

const PORTONE_SDK_URL = "https://cdn.portone.io/v2/browser-sdk.js";

export const PORTONE_STORE_ID = process.env.NEXT_PUBLIC_PORTONE_STORE_ID ?? null;
export const PORTONE_CHANNEL_KEY = process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY ?? null;

let sdkLoadPromise = null;

// PortOne v2 브라우저 SDK를 CDN에서 지연 로드한다 (기존 프로젝트가 Pretendard 폰트를
// CDN <link>로 불러오는 것과 동일한 방식 — 새 npm 의존성을 추가하지 않는다).
export function loadPortOneSdk() {
  if (typeof window === "undefined") {
    return Promise.reject(new Error("PortOne SDK는 브라우저 환경에서만 로드할 수 있습니다."));
  }
  if (window.PortOne) return Promise.resolve(window.PortOne);
  if (sdkLoadPromise) return sdkLoadPromise;

  sdkLoadPromise = new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = PORTONE_SDK_URL;
    script.async = true;
    script.onload = () => resolve(window.PortOne);
    script.onerror = () => {
      sdkLoadPromise = null;
      reject(new Error("PortOne SDK를 불러오지 못했습니다."));
    };
    document.head.appendChild(script);
  });
  return sdkLoadPromise;
}

/**
 * TODO(backend/PortOne 설정 확정 후 연결):
 * 백엔드가 생성한 주문(order)을 기준으로 PortOne 결제창을 호출한다.
 * order는 반드시 서버 응답(getOrder/createOrder 결과)에서 온 값을 그대로 사용해야 하며,
 * 프론트에서 amount 등을 재계산하거나 덮어쓰지 않는다.
 *
 * @param {{ order: { merchantOrderId: string, amount: number, orderName: string } }} params
 * @returns {Promise<{ paymentId: string, code?: string, message?: string }>}
 *   결제창이 반환하는 결과. 이 값만으로 성공을 확정하지 말고 반드시
 *   payment.verifyPayment({ orderId, paymentId })로 백엔드 검증을 거칠 것.
 */
export async function requestPayment({ order }) {
  await loadPortOneSdk();
  throw new Error(
    "NOT_IMPLEMENTED: PortOne storeId/channelKey 및 결제 요청 파라미터 확정 후 연결 예정입니다.",
  );
  // 연결 시 예상 형태 (PortOne v2 SDK 문서 기준으로 확정 필요):
  // return window.PortOne.requestPayment({
  //   storeId: PORTONE_STORE_ID,
  //   channelKey: PORTONE_CHANNEL_KEY,
  //   paymentId: order.merchantOrderId,
  //   orderName: order.orderName,
  //   totalAmount: order.amount, // 백엔드가 내려준 금액 그대로 사용
  //   currency: "KRW",
  //   payMethod: "CARD",
  // });
}

/**
 * TODO(backend/PortOne 설정 확정 후 연결):
 * 구독 자동결제용 빌링키 발급 창을 호출한다.
 * intent는 billingKey.createBillingKeyIssuanceIntent()의 응답(issueId 포함)이어야 한다.
 *
 * @param {{ intent: { issueId: string } }} params
 * @returns {Promise<{ billingKey: string }>}
 */
export async function requestIssueBillingKey({ intent }) {
  await loadPortOneSdk();
  throw new Error(
    "NOT_IMPLEMENTED: PortOne storeId/channelKey 및 빌링키 발급 파라미터 확정 후 연결 예정입니다.",
  );
  // 연결 시 예상 형태:
  // return window.PortOne.requestIssueBillingKey({
  //   storeId: PORTONE_STORE_ID,
  //   channelKey: PORTONE_CHANNEL_KEY,
  //   issueId: intent.issueId,
  //   issueName: "시티팜 구독 자동결제 카드 등록",
  // });
  // 발급된 billingKey 문자열은 여기서 로그로 남기거나 우리 서버 외의 곳에 저장하지 않는다.
}
