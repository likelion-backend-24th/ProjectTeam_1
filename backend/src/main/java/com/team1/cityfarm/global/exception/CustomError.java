package com.team1.cityfarm.global.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum CustomError {
    //공통 예외
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    //인증 인가 예외
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    AUTH_LOGIN_FAILED(HttpStatus.BAD_REQUEST, "아이디 또는 비밀번호가 일치하지 않습니다."),
    AUTH_PASSWORD_VALID(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    AUTH_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "이메일을 입력해주세요"),
    AUTH_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "비밀번호를 입력해주세요"),
    AUTH_DUPLICATED_EMAIL(HttpStatus.CONFLICT, "이미 사용중인 이메일입니다"),
    AUTH_DUPLICATED_NICKNAME(HttpStatus.CONFLICT, "이미 사용중인 닉네임입니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    EMAIL_REQUIRED(HttpStatus.NOT_FOUND, "이메일을 입력해주세요."),
    PASSWORD_REQUIRED(HttpStatus.NOT_FOUND, "비밀번호를 입력해주세요."),

    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다"),
    REPLY_NOT_FOUND(HttpStatus.NOT_FOUND, "답글을 찾을 수 없습니다"),

    // JWT 토큰 예외
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "요청 헤더에서 토큰을 찾을 수 없습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "기간이 만료된 토큰입니다. 다시 로그인해주세요."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),

    //작성자 본인 이외 수정/삭제 접근시
    BOARD_NOT_OWNER(HttpStatus.FORBIDDEN, "게시글 수정/삭제 권한이 없습니다."),
    COMMENT_NOT_OWNER(HttpStatus.FORBIDDEN, "댓글 수정/삭제 권한이 없습니다."),
    REPLY_NOT_OWNER(HttpStatus.FORBIDDEN, "답글 수정/삭제 권한이 없습니다."),

    BOARD_TYPE_ERROR(HttpStatus.BAD_REQUEST,"잘못된 검색 타입입니다."),
    USER_STATUS_ERROR(HttpStatus.BAD_REQUEST,"STATUS에서 정의하지 않은 활동상태입니다."),

    // 팔로우 예외
    FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "팔로우 정보를 찾을 수 없습니다."),
    FOLLOW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 팔로우한 사용자입니다."),
    FOLLOW_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신은 팔로우할 수 없습니다."),
    FOLLOW_NOT_OWNER(HttpStatus.FORBIDDEN, "본인의 팔로우만 취소할 수 있습니다."),

    //기타 예외
    PORTONE_BILLING_FAILED(HttpStatus.BAD_REQUEST, "빌링키 정기 결제 요청 실패"),
    PORTONE_CANCEL_FAILED(HttpStatus.BAD_REQUEST, "결제 취소 실패"),
    SUBSCRIPTION_PASS_NOT_FOUND(HttpStatus.NOT_FOUND, "보유중인 구독권을 찾을 수 없습니다"),
    DUPLICATE_SUBSCRIPTION(HttpStatus.CONFLICT, "이미 활성중인 구독 정보입니다"),
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "활성중인 구독 정보가 없습니다"),
    BILLING_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 빌링키가 존재하지 않습니다"),
    BILLING_KEY_STATUS_NOT_FOUND(HttpStatus.BAD_REQUEST,"빌링키 상태 오류"),
    BILLING_KEY_IN_USE(HttpStatus.CONFLICT, "구독 결제 예약이 걸려있어 카드를 삭제할 수 없습니다. 카드 변경 또는 구독 해지를 먼저 진행해주세요."),
    SUBSCRIPTION_PASS_EXHAUSTED(HttpStatus.BAD_REQUEST, "구독 수강권 잔여 횟수가 없습니다"),
    CANNOT_REFUND(HttpStatus.FORBIDDEN, "환불 불가"),
    DUPLICATE_SETTLEMENT(HttpStatus.CONFLICT, "이미 존재하는 정산 데이터"),
    PAYMENT_FAILED(HttpStatus.FORBIDDEN, "결제 검증 실패"),
    INVALID_PAYMENT_AMOUNT(HttpStatus.NOT_MODIFIED, "결제 금액 불일치"),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "올바르지 않은 주문 상태입니다"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다"),
    DUPLICATE_PAYMENT(HttpStatus.CONFLICT, "이미 처리된 결제처리 입니다"),
    ONE_DAY_CLASS_NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없는 클래스 입니다"),
    ALREADY_ENROLLED_CLASS(HttpStatus.BAD_REQUEST, "이미 수강중인 클래스입니다"),
    PAYMENT_NOT_FOUND(HttpStatus.NO_CONTENT, "결제 정보 조회 실패"),
    PORTONE_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "API 취소 요청 실패"),

    // 원데이클래스 예외
    CLASS_CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "클래스 정원이 초과되었습니다"),
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "수강 신청 내역을 찾을 수 없습니다"),
    ENROLLMENT_NOT_OWNER(HttpStatus.FORBIDDEN, "본인의 신청 내역만 접근할 수 있습니다"),
    NOT_HOST_ROLE(HttpStatus.FORBIDDEN, "호스트 권한이 필요합니다"),
    CLASS_NOT_OWNER(HttpStatus.FORBIDDEN, "본인이 개설한 클래스만 접근할 수 있습니다"),

    // 밭 임대 예외
    FARM_IMAGE_LIMIT(HttpStatus.BAD_REQUEST, "사진은 최대 5장까지 올릴 수 있습니다."),
    FARM_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 밭입니다."),

    // 빌링 예외
    BILLING_KEY_VERIFY_FAILED(HttpStatus.BAD_REQUEST, "빌링키 검증에 실패했습니다."),

    // 정산 예외
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "정산 정보를 찾을 수 없습니다."),
    ALREADY_COMPLETED_SETTLEMENT(HttpStatus.BAD_REQUEST, "이미 지급 완료된 정산 건입니다."),
    ALREADY_CANCELLED_SETTLEMENT(HttpStatus.BAD_REQUEST, "이미 취소된 정산 건입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}