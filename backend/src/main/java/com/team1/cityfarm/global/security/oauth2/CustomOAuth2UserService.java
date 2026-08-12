package com.team1.cityfarm.global.security.oauth2;

import com.team1.cityfarm.dto.oauth2.GoogleUserInfo;
import com.team1.cityfarm.dto.oauth2.KakaoUserInfo;
import com.team1.cityfarm.dto.oauth2.NaverUserInfo;
import com.team1.cityfarm.dto.oauth2.OAuth2UserInfo;
import com.team1.cityfarm.entity.ProviderType;
import com.team1.cityfarm.entity.RoleType;
import com.team1.cityfarm.entity.SocialAccount;
import com.team1.cityfarm.entity.User;
import com.team1.cityfarm.repository.SocialAccountRepository;
import com.team1.cityfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;

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
        } else if ("kakao".equalsIgnoreCase(registrationId)) {
            userInfo = new KakaoUserInfo(attributes);
        } else if ("naver".equalsIgnoreCase(registrationId)) {
            userInfo = new NaverUserInfo(attributes);
        }

        if (userInfo == null) {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인 제공자입니다.");
        }

        // 4. DB 저장 또는 기존 회원 정보 업데이트 및 소셜 계정 연동
        User user = saveOrUpdate(userInfo);

        // 5. CustomOAuth2User 반환 (OAuth2SuccessHandler에서 oAuth2User.getUser()를 직접 꺼낼 수 있게 설정)
        return new CustomOAuth2User(user, attributes);
    }

    private User saveOrUpdate(OAuth2UserInfo userInfo) {
        ProviderType provider = userInfo.getProvider();
        String providerId = userInfo.getProviderId();
        String email = userInfo.getEmail();

        // 1. SocialAccount 테이블에서 기존 연동 계정 조회
        return socialAccountRepository.findByProviderAndProviderId(provider, providerId)
                .map(SocialAccount::getUser)
                .orElseGet(() -> {
                    // 2. 연동된 소셜 계정이 없으면, 이메일로 기존 User 조회
                    User user = userRepository.findByEmail(email)
                            .orElseGet(() -> {
                                // 3. 신규 회원일 경우 User 생성
                                String tempNickname = "user_" + UUID.randomUUID().toString().substring(0, 8);
                                return userRepository.save(
                                        User.builder()
                                                .email(email)
                                                .name(userInfo.getName() != null ? userInfo.getName() : tempNickname)
                                                .nickname(tempNickname)
                                                .password(passwordEncoder.encode("OAUTH_USER_TEMP_PASSWORD"))
                                                .roleType(RoleType.USER)
                                                .build()
                                );
                            });

                    // 4. 조회되거나 새로 생성된 User에 SocialAccount 연동 저장
                    socialAccountRepository.save(
                            SocialAccount.builder()
                                    .user(user)
                                    .provider(provider)
                                    .providerId(providerId)
                                    .build()
                    );
                    return user;
                });
    }
}