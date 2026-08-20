package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.OneDayClass;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OneDayClassRepository extends JpaRepository<OneDayClass, Long> {

    // 수강신청 정원/중복 체크용 락 (동시 신청 시 정원 초과 방지)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OneDayClass c where c.id = :id")
    Optional<OneDayClass> findByIdForUpdate(@Param("id") Long id);
}
