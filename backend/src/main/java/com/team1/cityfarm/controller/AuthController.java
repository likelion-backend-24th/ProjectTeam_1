package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.LoginRequestDto;
import com.team1.cityfarm.dto.SignupRequestDto;
import com.team1.cityfarm.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원관리 API", description = "회원 가입 및 로그인 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "회원가입",
            description = "이메일, 비밀번호, 이름 및 닉네임 등의 정보를 받아 새로운 회원을 등록합니다.")
    @PostMapping("/signup")
    public ResponseEntity<String> siginup(@RequestBody SignupRequestDto signupRequestDto){
        authService.signUp(signupRequestDto);
        return ResponseEntity.ok("회원가입 성공");
    }

    @Operation(summary = "로그인",
            description = "입력된 이메일과 비밀번호를 확인해 로그인 승인 여부를 판단합니다.")
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDto loginRequestDto){
        authService.login(loginRequestDto);
        return ResponseEntity.ok("로그인 성공");
    }
}
