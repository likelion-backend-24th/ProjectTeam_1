package com.team1.cityfarm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PasswordCheckRequestDto {
    @NotBlank
    private String currentPassword;
}
