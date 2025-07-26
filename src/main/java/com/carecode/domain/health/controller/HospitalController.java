package com.carecode.domain.health.controller;

import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.health.repository.HospitalRepository;
import com.carecode.domain.health.service.HospitalLikeService;
import com.carecode.domain.health.service.HospitalService;
import com.carecode.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("/hospitals")
@RequiredArgsConstructor
public class HospitalController {
    private final HospitalRepository hospitalRepository;
    private final HospitalLikeService hospitalLikeService;
    private final HospitalService hospitalService;
    private final UserService userService;

    @GetMapping
    public List<Hospital> getAll() {
        return hospitalRepository.findAll();
    }

    @GetMapping("/{id}")
    public Hospital getById(@PathVariable Long id) {
        return hospitalRepository.findById(id).orElseThrow();
    }

    @GetMapping("/nearby")
    public List<Hospital> getNearby(@RequestParam double lat, @RequestParam double lng, @RequestParam double radius) {
        return hospitalRepository.findNearby(lat, lng, radius);
    }

    @GetMapping("/type/{type}")
    public List<Hospital> getByType(@PathVariable String type) {
        return hospitalRepository.findByType(type);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeHospital(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdFromUserDetails(userDetails);
        boolean alreadyLiked = hospitalLikeService.isLiked(id, userId);
        if (alreadyLiked) {
            return ResponseEntity.status(409).body("이미 좋아요를 누른 병원입니다.");
        }
        hospitalLikeService.likeHospital(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<?> unlikeHospital(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdFromUserDetails(userDetails);
        hospitalLikeService.unlikeHospital(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/likes")
    public long getLikeCount(@PathVariable Long id) {
        return hospitalLikeService.getLikeCount(id);
    }

    @GetMapping("/popular")
    public List<Hospital> getPopularHospitals(@RequestParam(defaultValue = "10") int limit) {
        return hospitalService.getHospitalsOrderByLikes().stream().limit(limit).toList();
    }
} 