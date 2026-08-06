package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.RoleType;
import com.team1.cityfarm.entity.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "관리자용 유저 정보 및 상태 변경 요청 DTO")
@Getter
public class UserRequestDto {
    //Admin 컨트롤러에서 상태 변경용으로 사용
    @Schema(description = "유저 계정 상태 (ACTIVE, INACTIVE, BLOCKED 등)", example = "ACTIVE")
    private Status status;

    @Schema(description = "유저 권한 유형 (ROLE_USER, ROLE_ADMIN 등)", example = "ROLE_USER")
    private RoleType roleType;
}