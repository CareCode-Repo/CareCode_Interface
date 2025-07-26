package com.carecode.domain.health.controller;

import com.carecode.domain.health.entity.HospitalReview;
import com.carecode.domain.health.service.HospitalReviewService;
import com.carecode.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/hospitals")
@RequiredArgsConstructor
public class HospitalReviewController {
    private final HospitalReviewService reviewService;
    private final UserService userService;

    @PostMapping("/{id}/reviews")
    public HospitalReview createReview(
            @PathVariable Long id,
            @RequestParam int rating,
            @RequestParam String content,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdFromUserDetails(userDetails);
        return reviewService.createReview(id, userId, rating, content);
    }

    @GetMapping("/{id}/reviews")
    public List<HospitalReview> getReviews(@PathVariable Long id) {
        return reviewService.getReviewsByHospital(id);
    }

    @PutMapping("/reviews/{reviewId}")
    public HospitalReview updateReview(
            @PathVariable Long reviewId,
            @RequestParam int rating,
            @RequestParam String content,
            @AuthenticationPrincipal UserDetails userDetails) {
        // 실제 서비스에서는 작성자 본인만 수정 가능하도록 체크 필요
        return reviewService.updateReview(reviewId, rating, content);
    }

    @DeleteMapping("/reviews/{reviewId}")
    public void deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {
        // 실제 서비스에서는 작성자 본인만 삭제 가능하도록 체크 필요
        reviewService.deleteReview(reviewId);
    }
} 