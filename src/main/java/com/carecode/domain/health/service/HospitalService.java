package com.carecode.domain.health.service;

import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.health.repository.HospitalRepository;
import com.carecode.domain.health.repository.HospitalLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HospitalService {
    private final HospitalRepository hospitalRepository;
    private final HospitalLikeRepository hospitalLikeRepository;

    public List<Hospital> getHospitalsOrderByLikes() {
        List<Hospital> hospitals = hospitalRepository.findAll();
        return hospitals.stream()
                .sorted((h1, h2) -> Long.compare(
                        hospitalLikeRepository.countByHospital(h2),
                        hospitalLikeRepository.countByHospital(h1)))
                .collect(Collectors.toList());
    }
} 