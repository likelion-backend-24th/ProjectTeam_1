package com.team1.cityfarm.dto.oauth2;

import com.team1.cityfarm.entity.ProviderType;

import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> profile;

    @SuppressWarnings("unchecked")
    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        this.profile = (kakaoAccount != null && kakaoAccount.get("profile") != null)
                ? (Map<String, Object>) kakaoAccount.get("profile")
                : null;
    }

    @Override
    public String getProviderId() {
        // 카카오는 id를 Long 또는 String 형태로 전달하므로 String.valueOf 처리
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public ProviderType getProvider() {
        return ProviderType.KAKAO;
    }

    @Override
    public String getEmail() {
        return kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
    }

    @Override
    public String getName() {
        return profile != null ? (String) profile.get("nickname") : null;
    }
}