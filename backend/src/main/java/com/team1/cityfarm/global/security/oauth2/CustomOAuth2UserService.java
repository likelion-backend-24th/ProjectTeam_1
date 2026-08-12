package com.team1.cityfarm.global.security.oauth2;

import com.team1.cityfarm.dto.oauth2.GoogleUserInfo;
import com.team1.cityfarm.dto.oauth2.OAuth2UserInfo;
import com.team1.cityfarm.entity.RoleType;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 소셜 로그인 API의 사용자 정보를 가져옵니다.
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. 어떤 소셜 서비스인지 식별합니다 (google, kakao, naver 등)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 3. Provider에 맞는 OAuth2UserInfo 객체를 생성합니다.
        OAuth2UserInfo userInfo = null;
        if ("google".equalsIgnoreCase(registrationId)) {
            userInfo = new GoogleUserInfo(attributes);
        }

        if (userInfo == null) {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인 제공자입니다.");
        }

        // 4. DB 저장 또는 기존 회원 정보 업데이트 (이메일 같을 시 자동 연동)
        User user = saveOrUpdate(userInfo);

        // 5. OAuth2User 객체 반환 (Spring Security가 SecurityContext에 저장)
        return oAuth2User;
    }

    private User saveOrUpdate(OAuth2UserInfo userInfo) {
        // 1. provider + providerId로 기존 소셜 회원 검색
        return userRepository.findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId())
                .map(entity -> {
                    entity.setName(userInfo.getName());
                    return entity;
                })
                // 2. 소셜 연동이 안 되어 있는 경우: 이메일로 기존 회원 검색하여 자동 연동
                .orElseGet(() -> userRepository.findByEmail(userInfo.getEmail())
                        .map(existingUser -> {
                            existingUser.linkSocial(userInfo.getProvider(), userInfo.getProviderId());
                            return existingUser;
                        })
                        // 3. 신규 회원일 경우 자동 가입
                        .orElseGet(() -> userRepository.save(
                                User.builder()
                                        .email(userInfo.getEmail())
                                        .name(userInfo.getName())
                                        .provider(userInfo.getProvider())
                                        .providerId(userInfo.getProviderId())
                                        .roleType(RoleType.USER)
                                        .build()
                        ))
                );
    }
}