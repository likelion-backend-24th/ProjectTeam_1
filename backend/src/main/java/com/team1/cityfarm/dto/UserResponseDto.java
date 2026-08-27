package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.RoleType;
import com.team1.cityfarm.entity.Status;
import com.team1.cityfarm.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "관리자용 사용자 정보 응답 DTO")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto {

    @Schema(description = "사용자 고유 ID", example = "1")
    private Long id;

    @Schema(description = "이메일 주소", example = "user@example.com")
    private String email;

    @Schema(description = "닉네임", example = "파밍마스터")
    private String nickname;

    @Schema(description = "실명", example = "홍길동")
    private String name;

    @Schema(description = "계정 상태 (ACTIVE, INACTIVE, SUSPENDED 등)", example = "ACTIVE")
    private Status status;

    @Schema(description = "사용자 권한 (USER, ADMIN 등)", example = "USER")
    private RoleType roleType;

    @Schema(description = "계정 생성 시각", example = "2026-08-01T12:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "계정 수정 시각", example = "2026-08-05T15:30:00")
    private LocalDateTime updatedAt;

    public static UserResponseDto from(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getName(),
                user.getStatus(),
                user.getRoleType(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}