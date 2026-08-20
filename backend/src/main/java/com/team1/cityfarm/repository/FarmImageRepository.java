package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.FarmImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FarmImageRepository extends JpaRepository <FarmImage, Long> {

    // 대표 사진을 첫번째로 함
    Optional<FarmImage> findFirstByFarm_IdOrderByIdAsc(Long farmId);
}
