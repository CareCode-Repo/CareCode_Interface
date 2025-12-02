package com.carecode.domain.health.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.domain.health.dto.request.HealthRequest;
import com.carecode.domain.health.dto.request.HealthCreateHealthRecordRequest;
import com.carecode.domain.health.dto.request.HealthUpdateHealthRecordRequest;
import com.carecode.domain.health.dto.request.HealthCreateHospitalReviewRequest;
import com.carecode.domain.health.dto.request.HealthUpdateHospitalReviewRequest;
import com.carecode.domain.health.dto.response.HealthResponse;
import com.carecode.domain.health.dto.response.HealthRecordResponse;
import com.carecode.domain.health.dto.response.VaccineScheduleResponse;
import com.carecode.domain.health.dto.response.CheckupScheduleResponse;
import com.carecode.domain.health.dto.response.HealthStatsResponse;
import com.carecode.domain.health.dto.response.HealthAlertResponse;
import com.carecode.domain.health.dto.response.HospitalInfoResponse;
import com.carecode.domain.health.dto.response.HospitalReviewResponse;
import com.carecode.domain.health.app.HealthFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 통합 건강 관리 컨트롤러
 * 건강 정보, 병원 정보, 병원 리뷰 등 모든 건강 관련 API
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "건강 관리", description = "통합 건강 관리 API (건강 정보, 병원, 리뷰)")
public class HealthController extends BaseController {

    private final HealthFacade healthFacade;

    // ==================== 건강 정보 관리 ====================

    /**
     * 건강 정보 등록
     */
    @PostMapping("/records")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 정보 등록", description = "새로운 건강 정보를 등록합니다.")
    public ResponseEntity<HealthRecordResponse> createHealthRecord(@Parameter(description = "건강 정보", required = true)
                                                                                     @RequestBody HealthCreateHealthRecordRequest request) {

        HealthRecordResponse record = healthFacade.createHealthRecord(request);

        return ResponseEntity.ok(record);
    }

    /**
     * 건강 정보 조회
     */
    @GetMapping("/records/{recordId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 정보 조회", description = "특정 건강 정보를 조회합니다.")
    public ResponseEntity<HealthRecordResponse> getHealthRecord(@Parameter(description = "건강 정보 ID", required = true) @PathVariable Long recordId) {

        HealthRecordResponse record = healthFacade.getHealthRecordById(recordId);

        return ResponseEntity.ok(record);
    }

    /**
     * 사용자별 건강 정보 조회
     */
    @GetMapping("/records/user/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자별 건강 정보 조회", description = "특정 사용자의 모든 건강 정보를 조회합니다.")
    public ResponseEntity<List<HealthRecordResponse>> getUserHealthRecords(@Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {

        List<HealthRecordResponse> records = healthFacade.getHealthRecordsByUserId(userId);

        return ResponseEntity.ok(records);
    }

    /**
     * 건강 정보 수정
     */
    @PutMapping("/records/{recordId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 정보 수정", description = "기존 건강 정보를 수정합니다.")
    public ResponseEntity<HealthRecordResponse> updateHealthRecord(@Parameter(description = "건강 정보 ID", required = true) @PathVariable Long recordId,
                                                                                  @Parameter(description = "수정할 건강 정보", required = true) @RequestBody HealthUpdateHealthRecordRequest request) {

        HealthRecordResponse record = healthFacade.updateHealthRecord(recordId, request);

        return ResponseEntity.ok(record);
    }

    /**
     * 건강 정보 삭제
     */
    @DeleteMapping("/records/{recordId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 정보 삭제", description = "건강 정보를 삭제합니다.")
    public ResponseEntity<Void> deleteHealthRecord(@Parameter(description = "건강 정보 ID", required = true) @PathVariable Long recordId) {

        healthFacade.deleteHealthRecord(recordId);

        return ResponseEntity.ok().build();
    }

    // ==================== 건강 통계 ====================

    /**
     * 건강 통계 조회
     */
    @GetMapping("/statistics")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 통계 조회", description = "사용자의 건강 관련 통계를 조회합니다.")
    public ResponseEntity<HealthStatsResponse> getHealthStatistics(@Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {

        HealthStatsResponse statistics = healthFacade.getHealthStatistics(userId);

        return ResponseEntity.ok(statistics);
    }

    /**
     * 예방접종 스케줄 조회
     */
    @GetMapping("/vaccines/schedule")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "예방접종 스케줄 조회", description = "아동의 예방접종 스케줄을 조회합니다.")
    public ResponseEntity<List<VaccineScheduleResponse>> getVaccineSchedule(@Parameter(description = "아동 ID", required = true) @RequestParam String childId) {

        List<VaccineScheduleResponse> schedule = healthFacade.getVaccineSchedule(childId);

        return ResponseEntity.ok(schedule);
    }

    /**
     * 건강 검진 스케줄 조회
     */
    @GetMapping("/checkups/schedule")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 검진 스케줄 조회", description = "아동의 건강 검진 스케줄을 조회합니다.")
    public ResponseEntity<List<CheckupScheduleResponse>> getCheckupSchedule(@Parameter(description = "아동 ID", required = true) @RequestParam String childId) {

        List<CheckupScheduleResponse> schedule = healthFacade.getCheckupSchedule(childId);

        return ResponseEntity.ok(schedule);
    }

    /**
     * 건강 알림 조회
     */
    @GetMapping("/alerts")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 알림 조회", description = "사용자의 건강 관련 알림을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<HealthAlertResponse>> getHealthAlerts(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {
        List<HealthAlertResponse> alerts = healthFacade.getHealthAlerts(userId);
        return ResponseEntity.ok(alerts);
    }

    // ==================== 병원 관리 ====================

    /**
     * 모든 병원 조회
     */
    @GetMapping("/hospitals")
    @LogExecutionTime
    @Operation(summary = "모든 병원 조회", description = "등록된 모든 병원 정보를 조회합니다.")
    public ResponseEntity<List<HospitalInfoResponse>> getAllHospitals() {
        List<HospitalInfoResponse> hospitals = healthFacade.getAllHospitals();
        return ResponseEntity.ok(hospitals);
    }

    /**
     * 병원 상세 조회
     */
    @GetMapping("/hospitals/{id}")
    @LogExecutionTime
    @Operation(summary = "병원 상세 조회", description = "특정 병원의 상세 정보를 조회합니다.")
    public ResponseEntity<HospitalInfoResponse> getHospitalById(@Parameter(description = "병원 ID", required = true) @PathVariable Long id) {

        HospitalInfoResponse hospital = healthFacade.getHospitalById(id);

        return ResponseEntity.ok(hospital);
    }

    /**
     * 근처 병원 조회
     */
    @GetMapping("/hospitals/nearby")
    @LogExecutionTime
    @Operation(summary = "근처 병원 조회", description = "위치 기반으로 근처 병원을 조회합니다.")
    public ResponseEntity<List<HospitalInfoResponse>> getNearbyHospitals(@Parameter(description = "위도", required = true) @RequestParam double lat,
                                                                            @Parameter(description = "경도", required = true) @RequestParam double lng,
                                                                            @Parameter(description = "반경(km)", required = true) @RequestParam double radius) {

        List<HospitalInfoResponse> hospitals = healthFacade.getNearbyHospitals(lat, lng, radius);

        return ResponseEntity.ok(hospitals);
    }

    /**
     * 병원 타입별 조회
     */
    @GetMapping("/hospitals/type/{type}")
    @LogExecutionTime
    @Operation(summary = "병원 타입별 조회", description = "특정 타입의 병원들을 조회합니다.")
    public ResponseEntity<List<HospitalInfoResponse>> getHospitalsByType(@Parameter(description = "병원 타입", required = true) @PathVariable String type) {

        List<HospitalInfoResponse> hospitals = healthFacade.getHospitalsByType(type);

        return ResponseEntity.ok(hospitals);
    }

    /**
     * 병원 좋아요
     */
    @PostMapping("/hospitals/{id}/like")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "병원 좋아요", description = "병원에 좋아요를 추가합니다.")
    public ResponseEntity<?> likeHospital(@Parameter(description = "병원 ID", required = true) @PathVariable Long id,
                                          @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId) {

        boolean success = healthFacade.likeHospital(id, userId);

        if (!success) {
            return ResponseEntity.status(409).body("이미 좋아요를 누른 병원입니다.");
        }

        return ResponseEntity.ok().build();
    }

    /**
     * 병원 좋아요 취소
     */
    @DeleteMapping("/hospitals/{id}/like")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "병원 좋아요 취소", description = "병원의 좋아요를 취소합니다.")
    public ResponseEntity<?> unlikeHospital(@Parameter(description = "병원 ID", required = true) @PathVariable Long id,
                                            @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId) {

        boolean success = healthFacade.unlikeHospital(id, userId);

        if (!success) {
            return ResponseEntity.status(409).body("좋아요를 누르지 않은 병원입니다.");
        }

        return ResponseEntity.ok().build();
    }

    /**
     * 병원 좋아요 수 조회
     */
    @GetMapping("/hospitals/{id}/likes")
    @LogExecutionTime
    @Operation(summary = "병원 좋아요 수 조회", description = "병원의 좋아요 수를 조회합니다.")
    public ResponseEntity<Long> getHospitalLikeCount(@Parameter(description = "병원 ID", required = true) @PathVariable Long id) {

        long likeCount = healthFacade.getLikeCount(id);

        return ResponseEntity.ok(likeCount);
    }

    /**
     * 인기 병원 조회
     */
    @GetMapping("/hospitals/popular")
    @LogExecutionTime
    @Operation(summary = "인기 병원 조회", description = "좋아요가 많은 인기 병원들을 조회합니다.")
    public ResponseEntity<List<HospitalInfoResponse>> getPopularHospitals(@Parameter(description = "조회할 개수", required = false) @RequestParam(defaultValue = "10") int limit) {

        List<HospitalInfoResponse> hospitals = healthFacade.getPopularHospitals(limit);

        return ResponseEntity.ok(hospitals);
    }

    // ==================== 병원 리뷰 관리 ====================

    /**
     * 병원 리뷰 작성
     */
    @PostMapping("/hospitals/{id}/reviews")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "병원 리뷰 작성", description = "병원에 리뷰를 작성합니다.")
    public ResponseEntity<HospitalInfoResponseReview> createHospitalReview(@Parameter(description = "병원 ID", required = true) @PathVariable Long id,
                                                                              @Parameter(description = "리뷰 정보", required = true) @RequestBody HealthCreateHospitalReviewRequest request,
                                                                              @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId) {

        HospitalInfoResponseReview review = healthFacade.createHospitalReview(id, userId, request.getRating(), request.getContent());

        return ResponseEntity.ok(review);
    }

    /**
     * 병원 리뷰 조회
     */
    @GetMapping("/hospitals/{id}/reviews")
    @LogExecutionTime
    @Operation(summary = "병원 리뷰 조회", description = "특정 병원의 모든 리뷰를 조회합니다.")
    public ResponseEntity<List<HospitalInfoResponseReview>> getHospitalReviews(@Parameter(description = "병원 ID", required = true) @PathVariable Long id) {

        List<HospitalInfoResponseReview> reviews = healthFacade.getHospitalReviews(id);

        return ResponseEntity.ok(reviews);
    }

    /**
     * 병원 리뷰 수정
     */
    @PutMapping("/hospitals/reviews/{reviewId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "병원 리뷰 수정", description = "기존 병원 리뷰를 수정합니다.")
    public ResponseEntity<HospitalInfoResponseReview> updateHospitalReview(@Parameter(description = "리뷰 ID", required = true) @PathVariable Long reviewId,
                                                                              @Parameter(description = "수정할 리뷰 정보", required = true) @RequestBody HealthUpdateHospitalReviewRequest request,
                                                                              @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId) {

        HospitalInfoResponseReview review = healthFacade.updateHospitalReview(reviewId, userId, request.getRating(), request.getContent());

        return ResponseEntity.ok(review);
    }

    /**
     * 병원 리뷰 삭제
     */
    @DeleteMapping("/hospitals/reviews/{reviewId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "병원 리뷰 삭제", description = "병원 리뷰를 삭제합니다.")
    public ResponseEntity<Void> deleteHospitalReview(@Parameter(description = "리뷰 ID", required = true) @PathVariable Long reviewId,
                                                     @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId) {

        healthFacade.deleteHospitalReview(reviewId, userId);

        return ResponseEntity.ok().build();
    }

}