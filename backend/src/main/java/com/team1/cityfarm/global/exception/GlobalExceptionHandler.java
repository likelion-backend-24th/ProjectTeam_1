package com.team1.cityfarm.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    //CustomError Enum으로 정의한 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<CustomResponse> handleCustomException(CustomException e) {
        CustomError customError = e.getCustomError();

        return ResponseEntity
                .status(customError.getHttpStatus())
                .body(CustomResponse.of(customError));
    }
}
