package com.team1.cityfarm.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // 스프링 컨테이너의 ObjectMapper 주입
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        CustomError errorCode = CustomError.AUTH_UNAUTHORIZED;

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(errorCode.getHttpStatus().value());

        ApiResponse<Object> apiResponse = ApiResponse.error("UNAUTHORIZED", errorCode.getMessage());

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}