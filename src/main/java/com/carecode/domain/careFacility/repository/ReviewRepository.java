package com.carecode.domain.careFacility.repository;

import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.Review;
import com.carecode.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByCareFacilityOrderByCreatedAtDesc(CareFacility facility);
    Optional<Review> findByIdAndUser(Long reviewId, User user);
    long countByCareFacility(CareFacility facility);
}
