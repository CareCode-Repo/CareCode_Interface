package com.carecode.domain.health.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.domain.health.dto.request.HealthCreateHealthRecordRequest;
import com.carecode.domain.health.dto.request.HealthRecordAttachmentRequest;
import com.carecode.domain.health.dto.request.HealthUpdateHealthRecordRequest;
import com.carecode.domain.health.dto.request.HealthCreateHospitalReviewRequest;
import com.carecode.domain.health.dto.request.HealthUpdateHospitalReviewRequest;
import com.carecode.domain.health.dto.response.*;
import com.carecode.domain.health.app.HealthFacade;
import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.core.handler.ApiSuccess;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Date;

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
    private final UserRepository userRepository;

    // ==================== 건강 정보 관리 ====================


    // 건강 정보 등록

    @PostMapping("/records")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 정보 등록", description = "새로운 건강 정보를 등록합니다.")
    public ResponseEntity<HealthRecordResponse> createHealthRecord(@Parameter(description = "건강 정보", required = true)
                                                                                     @RequestBody HealthCreateHealthRecordRequest request) {

        HealthRecordResponse record = healthFacade.createHealthRecord(request, getAuthenticatedUserPk());

        return ResponseEntity.ok(record);
    }


    // 건강 정보 조회

    @GetMapping("/records/{recordId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 정보 조회", description = "특정 건강 정보를 조회합니다.")
    public ResponseEntity<HealthRecordResponse> getHealthRecord(@Parameter(description = "건강 정보 ID", required = true) @PathVariable Long recordId) {

        HealthRecordResponse record = healthFacade.getHealthRecordById(recordId, getAuthenticatedUserPk());

        return ResponseEntity.ok(record);
    }


    // 사용자별 건강 정보 조회

    @GetMapping("/records/user/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자별 건강 정보 조회", description = "특정 사용자의 모든 건강 정보를 조회합니다.")
    public ResponseEntity<List<HealthRecordResponse>> getUserHealthRecords(@Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        List<HealthRecordResponse> records = healthFacade.getHealthRecordsByUserId(getAuthenticatedUserCode(), getAuthenticatedUserPk());

        return ResponseEntity.ok(records);
    }


    // 건강 정보 수정

    @PutMapping("/records/{recordId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 정보 수정", description = "기존 건강 정보를 수정합니다.")
    public ResponseEntity<HealthRecordResponse> updateHealthRecord(@Parameter(description = "건강 정보 ID", required = true) @PathVariable Long recordId,
                                                                                  @Parameter(description = "수정할 건강 정보", required = true) @RequestBody HealthUpdateHealthRecordRequest request) {

        HealthRecordResponse record = healthFacade.updateHealthRecord(recordId, request, getAuthenticatedUserPk());

        return ResponseEntity.ok(record);
    }


    // 건강 정보 삭제

    @DeleteMapping("/records/{recordId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 정보 삭제", description = "건강 정보를 삭제합니다.")
    public ResponseEntity<Void> deleteHealthRecord(@Parameter(description = "건강 정보 ID", required = true) @PathVariable Long recordId) {

        healthFacade.deleteHealthRecord(recordId, getAuthenticatedUserPk());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/records/{recordId}/attachments")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 기록 첨부 추가", description = "건강 기록에 첨부파일 메타 정보를 추가합니다.")
    public ResponseEntity<HealthRecordAttachmentResponse> addAttachment(@PathVariable Long recordId,
                                                                        @RequestBody HealthRecordAttachmentRequest request) {
        return ResponseEntity.ok(healthFacade.addAttachment(recordId, request, getAuthenticatedUserPk()));
    }

    @GetMapping("/records/{recordId}/attachments")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 기록 첨부 조회", description = "건강 기록의 첨부파일 목록을 조회합니다.")
    public ResponseEntity<List<HealthRecordAttachmentResponse>> getAttachments(@PathVariable Long recordId) {
        return ResponseEntity.ok(healthFacade.getAttachments(recordId, getAuthenticatedUserPk()));
    }

    @DeleteMapping("/records/attachments/{attachmentId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 기록 첨부 삭제", description = "건강 기록 첨부파일을 비활성화합니다.")
    public ResponseEntity<ApiSuccess> deleteAttachment(@PathVariable Long attachmentId) {
        healthFacade.deleteAttachment(attachmentId, getAuthenticatedUserPk());
        return ResponseEntity.ok(ApiSuccess.builder().timestamp(new Date()).message("첨부파일이 삭제되었습니다.").build());
    }

    // ==================== 건강 통계 ====================


    // 건강 통계 조회

    @GetMapping("/statistics")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 통계 조회", description = "사용자의 건강 관련 통계를 조회합니다.")
    public ResponseEntity<HealthStatsResponse> getHealthStatistics(@Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {
        HealthStatsResponse statistics = healthFacade.getHealthStatistics(getAuthenticatedUserCode(), getAuthenticatedUserPk());

        return ResponseEntity.ok(statistics);
    }


    // 예방접종 스케줄 조회

    @GetMapping("/vaccines/schedule")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "예방접종 스케줄 조회", description = "아동의 예방접종 스케줄을 조회합니다.")
    public ResponseEntity<List<VaccineScheduleResponse>> getVaccineSchedule(@Parameter(description = "아동 ID", required = true) @RequestParam String childId) {

        List<VaccineScheduleResponse> schedule = healthFacade.getVaccineSchedule(childId, getAuthenticatedUserPk());

        return ResponseEntity.ok(schedule);
    }


    // 건강 검진 스케줄 조회

    @GetMapping("/checkups/schedule")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 검진 스케줄 조회", description = "아동의 건강 검진 스케줄을 조회합니다.")
    public ResponseEntity<List<CheckupScheduleResponse>> getCheckupSchedule(@Parameter(description = "아동 ID", required = true) @RequestParam String childId) {

        List<CheckupScheduleResponse> schedule = healthFacade.getCheckupSchedule(childId, getAuthenticatedUserPk());

        return ResponseEntity.ok(schedule);
    }


    // 건강 알림 조회

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
        List<HealthAlertResponse> alerts = healthFacade.getHealthAlerts(getAuthenticatedUserCode(), getAuthenticatedUserPk());
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/recommendations")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "연계 추천 조회", description = "아동 연령 기반 정책/시설 연계 추천을 제공합니다.")
    public ResponseEntity<java.util.Map<String, Object>> getIntegratedRecommendations() {
        return ResponseEntity.ok(healthFacade.getIntegratedRecommendations(getAuthenticatedUserCode(), getAuthenticatedUserPk()));
    }

    // ==================== 병원 관리 ====================

    // 모든 병원 조회
    @GetMapping("/hospitals")
    @LogExecutionTime
    @Operation(summary = "모든 병원 조회", description = "등록된 모든 병원 정보를 조회합니다.")
    public ResponseEntity<List<HospitalInfoResponse>> getAllHospitals() {
        List<HospitalInfoResponse> hospitals = healthFacade.getAllHospitals();
        return ResponseEntity.ok(hospitals);
    }


    // 병원 상세 조회
    @GetMapping("/hospitals/{id}")
    @LogExecutionTime
    @Operation(summary = "병원 상세 조회", description = "특정 병원의 상세 정보를 조회합니다.")
    public ResponseEntity<HospitalInfoResponse> getHospitalById(@Parameter(description = "병원 ID", required = true) @PathVariable Long id) {

        HospitalInfoResponse hospital = healthFacade.getHospitalById(id);

        return ResponseEntity.ok(hospital);
    }


    // 근처 병원 조회
    @GetMapping("/hospitals/nearby")
    @LogExecutionTime
    @Operation(summary = "근처 병원 조회", description = "위치 기반으로 근처 병원을 조회합니다.")
    public ResponseEntity<List<HospitalInfoResponse>> getNearbyHospitals(@Parameter(description = "위도", required = true) @RequestParam double lat,
                                                                         @Parameter(description = "경도", required = true) @RequestParam double lng,
                                                                         @Parameter(description = "반경(km)", required = true) @RequestParam double radius) {

        List<HospitalInfoResponse> hospitals = healthFacade.getNearbyHospitals(lat, lng, radius);

        return ResponseEntity.ok(hospitals);
    }


    // 병원 타입별 조회
    @GetMapping("/hospitals/type/{type}")
    @LogExecutionTime
    @Operation(summary = "병원 타입별 조회", description = "특정 타입의 병원들을 조회합니다.")
    public ResponseEntity<List<HospitalInfoResponse>> getHospitalsByType(@Parameter(description = "병원 타입", required = true) @PathVariable String type) {

        List<HospitalInfoResponse> hospitals = healthFacade.getHospitalsByType(type);

        return ResponseEntity.ok(hospitals);
    }


    // 병원 좋아요

    @PostMapping("/hospitals/{id}/like")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "병원 좋아요", description = "병원에 좋아요를 추가합니다.")
    public ResponseEntity<?> likeHospital(@Parameter(description = "병원 ID", required = true) @PathVariable Long id,
                                          @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId) {
        boolean success = healthFacade.likeHospital(id, getAuthenticatedUserPk());

        if (!success) {
            return ResponseEntity.status(409).body("이미 좋아요를 누른 병원입니다.");
        }

        return ResponseEntity.ok().build();
    }


    // 병원 좋아요 취소
    @DeleteMapping("/hospitals/{id}/like")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "병원 좋아요 취소", description = "병원의 좋아요를 취소합니다.")
    public ResponseEntity<?> unlikeHospital(@Parameter(description = "병원 ID", required = true) @PathVariable Long id,
                                            @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId) {
        boolean success = healthFacade.unlikeHospital(id, getAuthenticatedUserPk());

        if (!success) {
            return ResponseEntity.status(409).body("좋아요를 누르지 않은 병원입니다.");
        }

        return ResponseEntity.ok().build();
    }


    // 병원 좋아요 수 조회
    @GetMapping("/hospitals/{id}/likes")
    @LogExecutionTime
    @Operation(summary = "병원 좋아요 수 조회", description = "병원의 좋아요 수를 조회합니다.")
    public ResponseEntity<Long> getHospitalLikeCount(@Parameter(description = "병원 ID", required = true) @PathVariable Long id) {

        long likeCount = healthFacade.getLikeCount(id);

        return ResponseEntity.ok(likeCount);
    }


    // 인기 병원 조회
    @GetMapping("/hospitals/popular")
    @LogExecutionTime
    @Operation(summary = "인기 병원 조회", description = "좋아요가 많은 인기 병원들을 조회합니다.")
    public ResponseEntity<List<HospitalInfoResponse>> getPopularHospitals(@Parameter(description = "조회할 개수", required = false) @RequestParam(defaultValue = "10") int limit) {

        List<HospitalInfoResponse> hospitals = healthFacade.getPopularHospitals(limit);

        return ResponseEntity.ok(hospitals);
    }

    // ==================== 병원 리뷰 관리 ====================

    // 병원 리뷰 작성
    @PostMapping("/hospitals/{id}/reviews")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "병원 리뷰 작성", description = "병원에 리뷰를 작성합니다.")
    public ResponseEntity<HospitalReviewResponse> createHospitalReview(@Parameter(description = "병원 ID", required = true) @PathVariable Long id,
                                                                       @Parameter(description = "리뷰 정보", required = true) @RequestBody HealthCreateHospitalReviewRequest request,
                                                                       @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId) {
        HospitalReviewResponse review = healthFacade.createHospitalReview(id, getAuthenticatedUserPk(), request.getRating(), request.getContent());

        return ResponseEntity.ok(review);
    }


    // 병원 리뷰 조회

    @GetMapping("/hospitals/{id}/reviews")
    @LogExecutionTime
    @Operation(summary = "병원 리뷰 조회", description = "특정 병원의 모든 리뷰를 조회합니다.")
    public ResponseEntity<List<HospitalReviewResponse>> getHospitalReviews(@Parameter(description = "병원 ID", required = true) @PathVariable Long id) {

        List<HospitalReviewResponse> reviews = healthFacade.getHospitalReviews(id);

        return ResponseEntity.ok(reviews);
    }


    // 병원 리뷰 수정

    @PutMapping("/hospitals/reviews/{reviewId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "병원 리뷰 수정", description = "기존 병원 리뷰를 수정합니다.")
    public ResponseEntity<HospitalReviewResponse> updateHospitalReview(@Parameter(description = "리뷰 ID", required = true) @PathVariable Long reviewId,
                                                                       @Parameter(description = "수정할 리뷰 정보", required = true) @RequestBody HealthUpdateHospitalReviewRequest request,
                                                                       @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId) {
        HospitalReviewResponse review = healthFacade.updateHospitalReview(reviewId, getAuthenticatedUserPk(), request.getRating(), request.getContent());

        return ResponseEntity.ok(review);
    }


    // 병원 리뷰 삭제

    @DeleteMapping("/hospitals/reviews/{reviewId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "병원 리뷰 삭제", description = "병원 리뷰를 삭제합니다.")
    public ResponseEntity<Void> deleteHospitalReview(@Parameter(description = "리뷰 ID", required = true) @PathVariable Long reviewId,
                                                     @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId) {
        healthFacade.deleteHospitalReview(reviewId, getAuthenticatedUserPk());

        return ResponseEntity.ok().build();
    }

    // ==================== 건강 기록 필터링 기능 ====================

    // 기간별 건강 기록 조회 (오래된순)
    @GetMapping("/records/date-range-asc")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "기간별 건강 기록 조회 (오래된순)", description = "특정 기간의 건강 기록을 오래된순으로 조회합니다.")
    public ResponseEntity<List<HealthRecordResponse>> getHealthRecordsByDateRangeAsc(@Parameter(description = "아동 ID", required = true) @RequestParam Long childId,
                                                                                     @Parameter(description = "시작일 (yyyy-MM-dd)", required = true) @RequestParam String startDate,
                                                                                     @Parameter(description = "종료일 (yyyy-MM-dd)", required = true) @RequestParam String endDate) {
        List<HealthRecordResponse> records = healthFacade.getHealthRecordsByDateRangeAsc(childId, LocalDate.parse(startDate), LocalDate.parse(endDate), getAuthenticatedUserPk());
        return ResponseEntity.ok(records);
    }

    // 타입별 건강 기록 조회
    @GetMapping("/records/type")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "타입별 건강 기록 조회", description = "특정 타입의 건강 기록을 조회합니다.")
    public ResponseEntity<List<HealthRecordResponse>> getHealthRecordsByType(@Parameter(description = "아동 ID", required = true) @RequestParam Long childId,
                                                                             @Parameter(description = "기록 타입 (VACCINATION, CHECKUP, MEDICATION, SYMPTOM, OTHER)", required = true) @RequestParam String recordType) {
        List<HealthRecordResponse> records = healthFacade.getHealthRecordsByType(childId, HealthRecord.RecordType.valueOf(recordType), getAuthenticatedUserPk());

        return ResponseEntity.ok(records);
    }

    // ==================== 자녀 관리 기능 ====================

    // 연령 범위별 자녀 조회
    @GetMapping("/children/age-range")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "연령 범위별 자녀 조회", description = "특정 연령 범위에 해당하는 자녀를 조회합니다.")
    public ResponseEntity<List<com.carecode.domain.health.dto.response.ChildInfoResponse>> getChildrenByAgeRange(@Parameter(description = "사용자 ID", required = true) @RequestParam Long userId, @Parameter(description = "최소 연령", required = true) @RequestParam Integer minAge, @Parameter(description = "최대 연령", required = true) @RequestParam Integer maxAge) {
        List<ChildInfoResponse> children = healthFacade.getChildrenByAgeRange(getAuthenticatedUserPk(), minAge, maxAge);

        return ResponseEntity.ok(children);
    }

    // 성별 자녀 조회
    @GetMapping("/children/gender")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "성별 자녀 조회", description = "특정 성별의 자녀를 조회합니다.")
    public ResponseEntity<List<com.carecode.domain.health.dto.response.ChildInfoResponse>> getChildrenByGender(@Parameter(description = "사용자 ID", required = true) @RequestParam Long userId,
                                                                                                               @Parameter(description = "성별 (MALE, FEMALE)", required = true) @RequestParam String gender) {
        List<ChildInfoResponse> children = healthFacade.getChildrenByGender(getAuthenticatedUserPk(), gender);
        return ResponseEntity.ok(children);
    }

    // 특별한 요구사항이 있는 자녀 조회
    @GetMapping("/children/special-needs")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "특별한 요구사항이 있는 자녀 조회", description = "특별한 요구사항이 있는 자녀를 조회합니다.")
    public ResponseEntity<List<com.carecode.domain.health.dto.response.ChildInfoResponse>> getChildrenWithSpecialNeeds(@Parameter(description = "사용자 ID", required = true) @RequestParam Long userId) {
        List<ChildInfoResponse> children = healthFacade.getChildrenWithSpecialNeeds(getAuthenticatedUserPk());

        return ResponseEntity.ok(children);
    }

    // 이름으로 자녀 검색
    @GetMapping("/children/search")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "이름으로 자녀 검색", description = "이름으로 자녀를 검색합니다.")
    public ResponseEntity<List<com.carecode.domain.health.dto.response.ChildInfoResponse>> searchChildrenByName(@Parameter(description = "사용자 ID", required = true) @RequestParam Long userId,
                                                                                                                @Parameter(description = "검색할 이름", required = true) @RequestParam String name) {
        List<ChildInfoResponse> children = healthFacade.searchChildrenByName(getAuthenticatedUserPk(), name);

        return ResponseEntity.ok(children);
    }

    private Long getAuthenticatedUserPk() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new CareServiceException("인증 사용자를 찾을 수 없습니다."));
        return user.getId();
    }

    private String getAuthenticatedUserCode() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new CareServiceException("인증 사용자를 찾을 수 없습니다."));
        return user.getUserId();
    }
}