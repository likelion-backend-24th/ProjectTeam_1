package com.team1.cityfarm.global.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
public class CustomResponse {
    private final HttpStatus httpStatus;
    private final String message;

    public static CustomResponse of(CustomError customError){
        return CustomResponse.builder()
                .httpStatus(customError.getHttpStatus())
                .message(customError.getMessage())
                .build();
    }
}
