package com.team1.cityfarm.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        CustomError errorCode = CustomError.AUTH_FORBIDDEN; // 권한 부족 에러

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(errorCode.getHttpStatus().value());

        ApiResponse<Object> apiResponse = ApiResponse.error("FORBIDDEN", errorCode.getMessage());

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}