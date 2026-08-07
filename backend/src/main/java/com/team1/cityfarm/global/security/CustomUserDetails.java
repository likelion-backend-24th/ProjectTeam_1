package com.team1.cityfarm.global.security;

import com.team1.cityfarm.entity.RoleType;
import com.team1.cityfarm.entity.Status;
import com.team1.cityfarm.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    private final RoleType roleType;
    private final Status status;

    // 1. DB 기반 생성자 (로그인 처리 시)
    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.roleType = user.getRoleType();
        this.status = user.getStatus();
    }

    // 2. 토큰 Claim 기반 생성자 (JwtAuthenticationFilter 전용)
    public CustomUserDetails(Long userId, String email, RoleType roleType) {
        this.userId = userId;
        this.email = email;
        this.roleType = roleType;
        this.status = getStatus();
    }

    // ROLE_USER 또는 ROLE_ADMIN 동적 부여
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleType.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == Status.ACTIVE;
    }
}