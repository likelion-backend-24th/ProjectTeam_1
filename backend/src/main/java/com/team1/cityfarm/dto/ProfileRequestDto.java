package com.team1.cityfarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "내 프로필 수정 DTO")
public class ProfileRequestDto {

    @Schema(description = "수정할 사용자 닉네임", example = "파머321")
    private String nickname;

    public ProfileRequestDto(String nickname) {
        this.nickname = nickname;
    }
}