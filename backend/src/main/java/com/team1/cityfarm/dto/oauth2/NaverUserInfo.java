package com.team1.cityfarm.dto.oauth2;
import com.team1.cityfarm.entity.ProviderType;

import java.util.Map;

public class NaverUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> response;

    @SuppressWarnings("unchecked")
    public NaverUserInfo(Map<String, Object> attributes) {
        // 네이버는 "response" 필드 안에 실제 사용자 정보가 들어있습니다.
        this.response = (Map<String, Object>) attributes.get("response");
    }

    @Override
    public String getProviderId() {
        return response != null ? (String) response.get("id") : null;
    }

    @Override
    public ProviderType getProvider() {
        return ProviderType.NAVER;
    }

    @Override
    public String getEmail() {
        return response != null ? (String) response.get("email") : null;
    }

    @Override
    public String getName() {
        return response != null ? (String) response.get("name") : null;
    }
}