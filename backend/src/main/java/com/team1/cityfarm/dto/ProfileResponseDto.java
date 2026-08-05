package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "사용자 프로필 응답 DTO")
public class ProfileResponseDto {

    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;

    @Schema(description = "사용자 닉네임", example = "상추러버123")
    private String nickName;

    @Schema(description = "사용자 이메일", example = "user@cityfarm.com")
    private String email;

    public ProfileResponseDto(User user) {
        this.name = user.getName();
        this.nickName = user.getNickname();
        this.email = user.getEmail();
    }
}