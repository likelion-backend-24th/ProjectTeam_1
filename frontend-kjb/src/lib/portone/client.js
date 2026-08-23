// PortOne 결제/빌링키 연동을 위한 유일한 진입점.
//
// 지켜야 할 원칙 (요구사항 14절):
// - 이 파일 밖에서는 절대 PortOne SDK를 직접 호출하지 않는다.
// - 여기서 다루는 storeId/channelKey는 공개 식별자이며 NEXT_PUBLIC_* 환경변수로만 관리한다.
// - API Secret 등 서버 전용 키는 이 프로젝트(프론트) 어디에도 절대 두지 않는다.
// - 이 모듈이 반환하는 결과는 "결제창 호출 결과"일 뿐 최종 결제 성공/실패가 아니다.
//   반드시 백엔드 검증(services/payment.verifyPayment 등)을 거친 뒤에만 화면에 최종 상태를 표시한다.

const PORTONE_SDK_URL = "https://cdn.portone.io/v2/browser-sdk.js";

// storeId/channelKey는 공개 식별자라 기본값을 여기 그대로 둔다(API_BASE_URL과 동일한 방식).
// 다른 값으로 테스트하고 싶으면 .env.local에서 NEXT_PUBLIC_PORTONE_* 로 덮어쓰면 된다.
export const PORTONE_STORE_ID =
  process.env.NEXT_PUBLIC_PORTONE_STORE_ID || "store-7bbf73b9-f318-4e1b-b3c4-731f79710136";
// 일반 결제(토스) 채널키
export const PORTONE_CHANNEL_KEY_NORMAL =
  process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY_NORMAL || "channel-key-c5723eb4-9ee3-4df3-9c56-129d13d4e9d6";
// 정기결제(빌링키) 채널키
export const PORTONE_CHANNEL_KEY_BILLING =
  process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY_BILLING || "channel-key-c0736ef7-925d-4de6-8f52-4cc89c502688";

// PortOne 이니시스 V2 채널이 결제/빌링키 발급 요청 모두에서 customer.phoneNumber를 필수로
// 요구한다(email, 구독은 fullName도 필수). 우리 시스템은 회원가입 때 전화번호를 아예 받지 않으므로
// 고정 placeholder를 사용한다 — SDK가 형식이 갖춰진 값을 요구할 뿐이라 실제 소유자 인증에는
// 쓰이지 않는 것으로 확인됨(2026-08-24 테스트). 실제 전화번호 수집 기능이 추가되면 이 상수 대신
// 로그인한 사용자의 값으로 교체할 것.
const PLACEHOLDER_PHONE_NUMBER = "01000000000";

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
 * 백엔드가 생성한 주문(order)을 기준으로 PortOne 결제창을 호출한다.
 * order는 반드시 서버 응답(getOrder/createOrder 결과)에서 온 값을 그대로 사용해야 하며,
 * 프론트에서 amount 등을 재계산하거나 덮어쓰지 않는다.
 *
 * @param {{ order: { merchantOrderId: string, amount: number, orderName?: string, title?: string }, profile?: { name?: string, email?: string } }} params
 * @returns {Promise<{ paymentId: string, txId?: string }>}
 *   결제창이 반환하는 결과. 이 값만으로 성공을 확정하지 말고 반드시
 *   payment.verifyPayment({ merchantOrderId, paymentId })로 백엔드 검증을 거칠 것.
 */
export async function requestPayment({ order, profile }) {
  const PortOne = await loadPortOneSdk();

  const response = await PortOne.requestPayment({
    storeId: PORTONE_STORE_ID,
    channelKey: PORTONE_CHANNEL_KEY_NORMAL,
    paymentId: order.merchantOrderId,
    orderName: order.orderName ?? order.title ?? "시티팜 결제",
    totalAmount: order.amount, // 백엔드가 내려준 금액 그대로 사용
    currency: "CURRENCY_KRW",
    payMethod: "CARD",
    customer: {
      email: profile?.email,
      fullName: profile?.name,
      phoneNumber: PLACEHOLDER_PHONE_NUMBER,
    },
  });

  // v2 SDK는 결제창에서 실패/취소가 나도 reject하지 않고 code가 채워진 결과로 resolve한다.
  if (response?.code) {
    throw new Error(response.message || "결제창에서 결제가 완료되지 않았어요.");
  }

  return response;
}

/**
 * 구독 자동결제용 빌링키 발급 창을 호출한다.
 * intent는 billingKey.createBillingKeyIssuanceIntent()의 응답(issueId, customerId 포함)이어야 한다.
 *
 * @param {{ intent: { issueId: string, customerId: string }, profile?: { name?: string, email?: string } }} params
 * @returns {Promise<{ billingKey: string }>}
 */
export async function requestIssueBillingKey({ intent, profile }) {
  const PortOne = await loadPortOneSdk();

  const response = await PortOne.requestIssueBillingKey({
    storeId: PORTONE_STORE_ID,
    channelKey: PORTONE_CHANNEL_KEY_BILLING,
    issueId: intent.issueId,
    issueName: "시티팜 구독 자동결제 카드 등록",
    customer: {
      customerId: intent.customerId,
      email: profile?.email,
      fullName: profile?.name,
      phoneNumber: PLACEHOLDER_PHONE_NUMBER,
    },
    billingKeyMethod: "CARD",
  });

  if (response?.code) {
    throw new Error(response.message || "카드 등록이 완료되지 않았어요.");
  }

  // 발급된 billingKey 문자열은 여기서 로그로 남기거나 우리 서버 외의 곳에 저장하지 않는다.
  return response;
}
