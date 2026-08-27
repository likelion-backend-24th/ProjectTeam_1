package com.team1.cityfarm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenRequestDto {
    @NotBlank(message = "refreshToken은 필수 입력값입니다.")
    private String refreshToken;
}
