package com.carecode.domain.health.repository;

import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.health.entity.HospitalLike;
import com.carecode.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalLikeRepository extends JpaRepository<HospitalLike, Long> {
    long countByHospital(Hospital hospital);
    boolean existsByHospitalAndUser(Hospital hospital, User user);
    void deleteByHospitalAndUser(Hospital hospital, User user);
} 