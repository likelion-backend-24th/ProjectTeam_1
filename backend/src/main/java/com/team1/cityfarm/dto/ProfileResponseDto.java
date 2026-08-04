package com.team1.cityfarm.dto;

public class ProfileResponseDto {
    private String name;
    private String nickName;
    private String email;

    public ProfileResponseDto(User user) {
        this.name = user.getName();
        this.nickName = user.getNickName();
        this.email = user.getEmail();
    }
}
