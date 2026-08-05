package com.team1.cityfarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "로그인 응답 DTO")
public class LoginResponseDto {

    @Schema(description = "사용자 이메일", example = "user@cityfarm.com")
    private String email;

    // 근데 패스워드는 왜..?
    @Schema(description = "비밀번호", example = "Password123!")
    private String password;
}