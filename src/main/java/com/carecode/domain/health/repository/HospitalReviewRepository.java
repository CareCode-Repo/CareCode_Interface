package com.carecode.domain.health.repository;

import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.health.entity.HospitalReview;
import com.carecode.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HospitalReviewRepository extends JpaRepository<HospitalReview, Long> {
    List<HospitalReview> findByHospital(Hospital hospital);
    List<HospitalReview> findByUser(User user);
}
