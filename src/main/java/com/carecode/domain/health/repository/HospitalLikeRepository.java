package com.carecode.domain.health.repository;

import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.health.entity.HospitalLike;
import com.carecode.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalLikeRepository extends JpaRepository<HospitalLike, Long> {
    long countByHospitalId(Long hospitalId);
    boolean existsByHospitalIdAndUserId(Long hospitalId, Long userId);
    void deleteByHospitalIdAndUserId(Long hospitalId, Long userId);
} 