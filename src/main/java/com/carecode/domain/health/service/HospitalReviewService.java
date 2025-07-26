package com.carecode.domain.health.service;

import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.health.entity.HospitalReview;
import com.carecode.domain.health.repository.HospitalRepository;
import com.carecode.domain.health.repository.HospitalReviewRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HospitalReviewService {
    private final HospitalReviewRepository reviewRepository;
    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;

    @Transactional
    public HospitalReview createReview(Long hospitalId, Long userId, int rating, String content) {
        Hospital hospital = hospitalRepository.findById(hospitalId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        HospitalReview review = HospitalReview.builder()
                .hospital(hospital)
                .user(user)
                .rating(rating)
                .content(content)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return reviewRepository.save(review);
    }

    @Transactional
    public HospitalReview updateReview(Long reviewId, int rating, String content) {
        HospitalReview review = reviewRepository.findById(reviewId).orElseThrow();
        review.setRating(rating);
        review.setContent(content);
        review.setUpdatedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        reviewRepository.deleteById(reviewId);
    }

    public List<HospitalReview> getReviewsByHospital(Long hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId).orElseThrow();
        return reviewRepository.findByHospital(hospital);
    }

    public List<HospitalReview> getReviewsByUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return reviewRepository.findByUser(user);
    }
} 