package com.team1.cityfarm.global.exception;

public class CustomException extends RuntimeException {
    private final CustomError customError;

    public CustomException(CustomError customError) {
        super(customError.getMessage());
        this.customError = customError;
    }

    public CustomError getCustomError() {
        return customError;
    }
}
