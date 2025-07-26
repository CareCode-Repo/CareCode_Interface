package com.carecode.domain.health.service;

import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.health.entity.HospitalLike;
import com.carecode.domain.health.repository.HospitalLikeRepository;
import com.carecode.domain.health.repository.HospitalRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HospitalLikeService {
    private final HospitalLikeRepository hospitalLikeRepository;
    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;

    @Transactional
    public void likeHospital(Long hospitalId, Long userId) {
        Hospital hospital = hospitalRepository.findById(hospitalId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        if (!hospitalLikeRepository.existsByHospitalAndUser(hospital, user)) {
            HospitalLike like = HospitalLike.builder()
                    .hospital(hospital)
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .build();
            hospitalLikeRepository.save(like);
        }
    }

    @Transactional
    public void unlikeHospital(Long hospitalId, Long userId) {
        Hospital hospital = hospitalRepository.findById(hospitalId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        hospitalLikeRepository.deleteByHospitalAndUser(hospital, user);
    }

    public long getLikeCount(Long hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId).orElseThrow();
        return hospitalLikeRepository.countByHospital(hospital);
    }

    public boolean isLiked(Long hospitalId, Long userId) {
        Hospital hospital = hospitalRepository.findById(hospitalId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        return hospitalLikeRepository.existsByHospitalAndUser(hospital, user);
    }
} 