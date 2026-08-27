package com.team1.cityfarm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PasswordChangeRequestDto {
    @NotBlank
    private String newPassword;
}
