package com.carecode.domain.health.repository;

import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.health.entity.HospitalLike;
import com.carecode.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalLikeRepository extends JpaRepository<HospitalLike, Long> {
    long countByHospital(Hospital hospital);
    long countByHospitalId(Long hospitalId);
    boolean existsByHospitalAndUser(Hospital hospital, User user);
    boolean existsByHospitalIdAndUserId(Long hospitalId, Long userId);
    void deleteByHospitalAndUser(Hospital hospital, User user);
    void deleteByHospitalIdAndUserId(Long hospitalId, Long userId);
} 