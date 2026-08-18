package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.ProviderType;
import com.team1.cityfarm.entity.SocialAccount;
import com.team1.cityfarm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    // provider와 providerId로 소셜 계정 조회 (기존 연동 여부 확인용)
    Optional<SocialAccount> findByProviderAndProviderId(ProviderType provider, String providerId);

    // 특정 유저에게 이미 동일한 provider 연동이 되어있는지 확인용
    boolean existsByUserAndProvider(User user, ProviderType provider);
}