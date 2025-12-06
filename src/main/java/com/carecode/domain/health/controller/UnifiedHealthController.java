package com.carecode.domain.health.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.domain.health.dto.*;
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
public class UnifiedHealthController extends BaseController {

    private final HealthFacade healthFacade;

    // ==================== 건강 정보 관리 ====================

    /**
     * 건강 정보 등록
     */
    @PostMapping("/records")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 정보 등록", description = "새로운 건강 정보를 등록합니다.")
    public ResponseEntity<HealthResponseDto.HealthRecordResponse> createHealthRecord(@Parameter(description = "건강 정보", required = true)
                                                                                     @RequestBody HealthRequestDto.CreateHealthRecordRequest request) {
        HealthResponseDto.HealthRecordResponse record = healthFacade.createHealthRecord(request);
        return ResponseEntity.ok(record);
    }

    /**
     * 건강 정보 조회
     */
    @GetMapping("/records/{recordId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 정보 조회", description = "특정 건강 정보를 조회합니다.")
    public ResponseEntity<HealthResponseDto.HealthRecordResponse> getHealthRecord(@Parameter(description = "건강 정보 ID", required = true) @PathVariable Long recordId) {
        HealthResponseDto.HealthRecordResponse record = healthFacade.getHealthRecordById(recordId);
        return ResponseEntity.ok(record);
    }

    /**
     * 사용자별 건강 정보 조회
     */
    @GetMapping("/records/user/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자별 건강 정보 조회", description = "특정 사용자의 모든 건강 정보를 조회합니다.")
    public ResponseEntity<List<HealthResponseDto.HealthRecordResponse>> getUserHealthRecords(@Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        List<HealthResponseDto.HealthRecordResponse> records = healthFacade.getHealthRecordsByUserId(userId);
        return ResponseEntity.ok(records);
    }

    /**
     * 건강 정보 수정
     */
    @PutMapping("/records/{recordId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 정보 수정", description = "기존 건강 정보를 수정합니다.")
    public ResponseEntity<HealthResponseDto.HealthRecordResponse> updateHealthRecord(@Parameter(description = "건강 정보 ID", required = true) @PathVariable Long recordId,
                                                                                     @Parameter(description = "수정할 건강 정보", required = true) @RequestBody HealthRequestDto.UpdateHealthRecordRequest request) {
        HealthResponseDto.HealthRecordResponse record = healthFacade.updateHealthRecord(recordId, request);
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
    public ResponseEntity<HealthResponseDto.HealthStatsResponse> getHealthStatistics(@Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {
        HealthResponseDto.HealthStatsResponse statistics = healthFacade.getHealthStatistics(userId);
        return ResponseEntity.ok(statistics);
    }

    /**
     * 예방접종 스케줄 조회
     */
    @GetMapping("/vaccines/schedule")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "예방접종 스케줄 조회", description = "아동의 예방접종 스케줄을 조회합니다.")
    public ResponseEntity<List<HealthResponseDto.VaccineScheduleResponse>> getVaccineSchedule(@Parameter(description = "아동 ID", required = true) @RequestParam String childId) {
        List<HealthResponseDto.VaccineScheduleResponse> schedule = healthFacade.getVaccineSchedule(childId);
        return ResponseEntity.ok(schedule);
    }

    /**
     * 건강 검진 스케줄 조회
     */
    @GetMapping("/checkups/schedule")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 검진 스케줄 조회", description = "아동의 건강 검진 스케줄을 조회합니다.")
    public ResponseEntity<List<HealthResponseDto.CheckupScheduleResponse>> getCheckupSchedule(
            @Parameter(description = "아동 ID", required = true) @RequestParam String childId) {
        List<HealthResponseDto.CheckupScheduleResponse> schedule = healthFacade.getCheckupSchedule(childId);
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
    public ResponseEntity<List<HealthResponseDto.HealthAlertResponse>> getHealthAlerts(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {
        List<HealthResponseDto.HealthAlertResponse> alerts = healthFacade.getHealthAlerts(userId);
        return ResponseEntity.ok(alerts);
    }

    // ==================== 병원 관리 ====================

    /**
     * 모든 병원 조회
     */
    @GetMapping("/hospitals")
    @LogExecutionTime
    @Operation(summary = "모든 병원 조회", description = "등록된 모든 병원 정보를 조회합니다.")
    public ResponseEntity<List<HospitalDto>> getAllHospitals() {
        List<HospitalDto> hospitals = healthFacade.getAllHospitals();
        return ResponseEntity.ok(hospitals);
    }

    /**
     * 병원 상세 조회
     */
    @GetMapping("/hospitals/{id}")
    @LogExecutionTime
    @Operation(summary = "병원 상세 조회", description = "특정 병원의 상세 정보를 조회합니다.")
    public ResponseEntity<HospitalDto> getHospitalById(@Parameter(description = "병원 ID", required = true) @PathVariable Long id) {
        HospitalDto hospital = healthFacade.getHospitalById(id);
        return ResponseEntity.ok(hospital);
    }

    /**
     * 근처 병원 조회
     */
    @GetMapping("/hospitals/nearby")
    @LogExecutionTime
    @Operation(summary = "근처 병원 조회", description = "위치 기반으로 근처 병원을 조회합니다.")
    public ResponseEntity<List<HospitalDto>> getNearbyHospitals(
            @Parameter(description = "위도", required = true) @RequestParam double lat,
            @Parameter(description = "경도", required = true) @RequestParam double lng,
            @Parameter(description = "반경(km)", required = true) @RequestParam double radius) {
        List<HospitalDto> hospitals = healthFacade.getNearbyHospitals(lat, lng, radius);
        return ResponseEntity.ok(hospitals);
    }

    /**
     * 병원 타입별 조회
     */
    @GetMapping("/hospitals/type/{type}")
    @LogExecutionTime
    @Operation(summary = "병원 타입별 조회", description = "특정 타입의 병원들을 조회합니다.")
    public ResponseEntity<List<HospitalDto>> getHospitalsByType(@Parameter(description = "병원 타입", required = true) @PathVariable String type) {
        List<HospitalDto> hospitals = healthFacade.getHospitalsByType(type);
        return ResponseEntity.ok(hospitals);
    }

    /**
     * 병원 좋아요
     */
    @PostMapping("/hospitals/{id}/like")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "병원 좋아요", description = "병원에 좋아요를 추가합니다.")
    public ResponseEntity<?> likeHospital(@Parameter(description = "병원 ID", required = true) @PathVariable Long id) {
        boolean success = healthFacade.likeHospital(id);
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
    public ResponseEntity<Void> unlikeHospital(@Parameter(description = "병원 ID", required = true) @PathVariable Long id) {
        healthFacade.unlikeHospital(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 병원 좋아요 수 조회
     */
    @GetMapping("/hospitals/{id}/likes")
    @LogExecutionTime
    @Operation(summary = "병원 좋아요 수 조회", description = "병원의 좋아요 수를 조회합니다.")
    public ResponseEntity<Long> getHospitalLikeCount(@Parameter(description = "병원 ID", required = true) @PathVariable Long id) {
        long likeCount = healthFacade.getHospitalLikeCount(id);
        return ResponseEntity.ok(likeCount);
    }

    /**
     * 인기 병원 조회
     */
    @GetMapping("/hospitals/popular")
    @LogExecutionTime
    @Operation(summary = "인기 병원 조회", description = "좋아요가 많은 인기 병원들을 조회합니다.")
    public ResponseEntity<List<HospitalDto>> getPopularHospitals(
            @Parameter(description = "조회할 개수", required = false) @RequestParam(defaultValue = "10") int limit) {
        List<HospitalDto> hospitals = healthFacade.getPopularHospitals(limit);
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
    public ResponseEntity<HospitalReviewDto> createHospitalReview(
            @Parameter(description = "병원 ID", required = true) @PathVariable Long id,
            @Parameter(description = "리뷰 정보", required = true) @RequestBody HospitalReviewRequestDto request) {
        HospitalReviewDto review = healthFacade.createHospitalReview(id, request);
        return ResponseEntity.ok(review);
    }

    /**
     * 병원 리뷰 조회
     */
    @GetMapping("/hospitals/{id}/reviews")
    @LogExecutionTime
    @Operation(summary = "병원 리뷰 조회", description = "특정 병원의 모든 리뷰를 조회합니다.")
    public ResponseEntity<List<HospitalReviewDto>> getHospitalReviews(@Parameter(description = "병원 ID", required = true) @PathVariable Long id) {
        List<HospitalReviewDto> reviews = healthFacade.getHospitalReviews(id);
        return ResponseEntity.ok(reviews);
    }

    /**
     * 병원 리뷰 수정
     */
    @PutMapping("/hospitals/reviews/{reviewId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "병원 리뷰 수정", description = "기존 병원 리뷰를 수정합니다.")
    public ResponseEntity<HospitalReviewDto> updateHospitalReview(
            @Parameter(description = "리뷰 ID", required = true) @PathVariable Long reviewId,
            @Parameter(description = "수정할 리뷰 정보", required = true) @RequestBody HospitalReviewRequestDto request) {
        HospitalReviewDto review = healthFacade.updateHospitalReview(reviewId, request);
        return ResponseEntity.ok(review);
    }

    /**
     * 병원 리뷰 삭제
     */
    @DeleteMapping("/hospitals/reviews/{reviewId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "병원 리뷰 삭제", description = "병원 리뷰를 삭제합니다.")
    public ResponseEntity<Void> deleteHospitalReview(@Parameter(description = "리뷰 ID", required = true) @PathVariable Long reviewId) {
        healthFacade.deleteHospitalReview(reviewId);
        return ResponseEntity.ok().build();
    }

    // ==================== 건강 분석 및 리포트 ====================

    /**
     * 건강 상태 분석
     */
    @PostMapping("/analysis")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 상태 분석", description = "아동의 건강 상태를 분석합니다.")
    public ResponseEntity<Map<String, Object>> analyzeHealthStatus(
            @Parameter(description = "건강 기록 정보", required = true) @RequestBody HealthRequest.CreateHealthRecord request) {
        Map<String, Object> analysis = healthFacade.analyzeHealthStatus(request);
        return ResponseEntity.ok(analysis);
    }

    /**
     * 건강 리포트 생성
     */
    @PostMapping("/reports")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 리포트 생성", description = "아동의 건강 리포트를 생성합니다.")
    public ResponseEntity<Map<String, Object>> generateHealthReport(
            @Parameter(description = "건강 기록 정보", required = true) @RequestBody HealthRequest.CreateHealthRecord request) {
        Map<String, Object> report = healthFacade.generateHealthReport(request);
        return ResponseEntity.ok(report);
    }

    /**
     * 건강 목표 조회
     */
    @GetMapping("/goals")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 목표 조회", description = "사용자의 건강 목표를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getHealthGoals(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {
        Map<String, Object> goals = healthFacade.getHealthGoals(userId);
        return ResponseEntity.ok(goals);
    }

    /**
     * 건강 차트 데이터 조회
     */
    @GetMapping("/charts")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 차트 데이터 조회", description = "건강 관련 차트 데이터를 조회합니다.")
    public ResponseEntity<List<Map<String, Object>>> getHealthChart(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
            @Parameter(description = "차트 타입", required = true) @RequestParam String type,
            @Parameter(description = "시작 날짜", required = false) @RequestParam(required = false) String from,
            @Parameter(description = "종료 날짜", required = false) @RequestParam(required = false) String to) {
        List<Map<String, Object>> chartData = healthFacade.getHealthChart(userId, type, from, to);
        return ResponseEntity.ok(chartData);
    }

    /**
     * 시스템 상태 확인
     */
    @GetMapping("/system/health")
    @LogExecutionTime
    @Operation(summary = "시스템 상태 확인", description = "건강 관리 시스템의 상태를 확인합니다.")
    public ResponseEntity<Map<String, Object>> checkSystemHealth() {
        Map<String, Object> healthStatus = healthFacade.checkSystemHealth();
        return ResponseEntity.ok(healthStatus);
    }
}