package com.team1.cityfarm.global.response;

public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ==================== 성공 팩토리 메서드 ====================
    // 반환값이 있을 때
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "SUCCESS", message, data);
    }

    // 반환값이 없을 때
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, "SUCCESS", message, null);
    }

    // ==================== 에러 팩토리 메서드 ====================
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}