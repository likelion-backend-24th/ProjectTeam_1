package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.User;

public class ProfileResponseDto {
    private String name;
    private String nickName;
    private String email;

    public ProfileResponseDto(User user) {
        this.name = user.getName();
        this.nickName = user.getNickname();
        this.email = user.getEmail();
    }
}
