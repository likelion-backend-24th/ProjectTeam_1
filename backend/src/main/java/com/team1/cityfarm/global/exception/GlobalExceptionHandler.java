package com.team1.cityfarm.global.exception;

import com.team1.cityfarm.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    //CustomError Enum으로 정의한 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomException(CustomException e) {
        CustomError customError = e.getCustomError();

        return ResponseEntity
                .status(customError.getHttpStatus())
                .body(ApiResponse.error(customError.name(), customError.getMessage()));
    }

    // 알 수 없는 서버 내부 에러(500) 일괄 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
    }
}
