package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.RoleType;
import com.team1.cityfarm.entity.Status;
import com.team1.cityfarm.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto {
    //admin 컨트롤러에서 정보조회용으로 사용

    private Long id;
    private String email;
    private String nickname;
    private String name;
    private Status status;
    private RoleType roleType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponseDto from(User user){
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
