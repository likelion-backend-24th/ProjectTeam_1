package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.RoleType;
import com.team1.cityfarm.entity.Status;
import lombok.Getter;

@Getter
public class UserRequestDto {
    //Admin 컨트롤러에서 상태 변경용으로 사용
    private Status status;
    private RoleType roleType;
}