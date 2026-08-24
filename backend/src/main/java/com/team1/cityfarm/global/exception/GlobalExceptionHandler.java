package com.team1.cityfarm.global.exception;

import com.team1.cityfarm.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
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

    // 업로드 용량 제한(spring.servlet.multipart.max-file-size 등) 초과 시 처리
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<?>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        CustomError customError = CustomError.FILE_TOO_LARGE;

        return ResponseEntity
                .status(customError.getHttpStatus())
                .body(ApiResponse.error(customError.name(), customError.getMessage()));
    }

    // @Valid 검증 실패(400) 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("잘못된 입력값입니다.");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(CustomError.INVALID_INPUT_VALUE.name(), message));
    }

    // 그 외 예외 처리 - 스프링이 이미 올바른 상태코드를 알고 있는 프레임워크 예외
    // (404 NoResourceFoundException, 405 HttpRequestMethodNotSupportedException 등,
    // Spring Framework 6부터 ErrorResponse를 구현함)는 그 상태코드를 그대로 살려서 응답하고,
    // 그 외 진짜 예상 못한 오류만 500으로 처리한다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
        if (e instanceof ErrorResponse errorResponse) {
            log.warn("[프레임워크 예외] status: {}, message: {}", errorResponse.getStatusCode(), e.getMessage());
            return ResponseEntity
                    .status(errorResponse.getStatusCode())
                    .body(ApiResponse.error(errorResponse.getStatusCode().toString(), e.getMessage()));
        }

        log.error("[처리되지 않은 서버 오류]", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
    }
    
}
