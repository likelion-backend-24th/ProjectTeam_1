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
    AUTH_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST,"이메일을 입력해주세요"),
    AUTH_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST,"비밀번호를 입력해주세요"),
    AUTH_DUPLICATED_EMAIL(HttpStatus.CONFLICT,"이미 사용중인 이메일입니다"),
    AUTH_DUPLICATED_NICKNAME(HttpStatus.CONFLICT,"이미 사용중인 닉네임입니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,"사용자를 찾을 수 없습니다"),

    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND,"댓글을 찾을 수 없습니다"),
    REPLY_NOT_FOUND(HttpStatus.NOT_FOUND,"답글을 찾을 수 없습니다"),

    // JWT 토큰 예외
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "요청 헤더에서 토큰을 찾을 수 없습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "기간이 만료된 토큰입니다. 다시 고르인해주세요."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),

    //작성자 본인 이외 수정/삭제 접근시
    BOARD_NOT_OWNER(HttpStatus.FORBIDDEN, "게시글 수정/삭제 권한이 없습니다."),
    COMMENT_NOT_OWNER(HttpStatus.FORBIDDEN, "댓글 수정/삭제 권한이 없습니다."),
    REPLY_NOT_OWNER(HttpStatus.FORBIDDEN, "답글 수정/삭제 권한이 없습니다."),

    BOARD_TYPE_ERROR(HttpStatus.BAD_REQUEST,"잘못된 검색 타입입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}