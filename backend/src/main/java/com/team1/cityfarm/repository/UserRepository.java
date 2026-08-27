package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.ProviderType;
import com.team1.cityfarm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByNickname(String nickname);

    boolean existsByNickname(String nickname);

    List<User> id(Long id);
}
