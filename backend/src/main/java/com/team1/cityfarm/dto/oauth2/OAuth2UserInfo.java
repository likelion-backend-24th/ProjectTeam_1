package com.team1.cityfarm.dto.oauth2;


import com.team1.cityfarm.entity.ProviderType;

public interface OAuth2UserInfo {
    ProviderType getProvider();
    String getProviderId();
    String getEmail();
    String getName();
}