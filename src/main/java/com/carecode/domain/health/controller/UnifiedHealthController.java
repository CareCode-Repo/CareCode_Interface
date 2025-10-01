package com.carecode.domain.health.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.health.dto.*;
import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    private final HealthService healthService;

    // ==================== 건강 정보 관리 ====================

    /**
     * 건강 정보 등록
     */
    @PostMapping("/records")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 정보 등록", description = "새로운 건강 정보를 등록합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "등록 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<HealthResponseDto.HealthRecordResponse> createHealthRecord(
            @Parameter(description = "건강 정보", required = true) 
            @RequestBody HealthRequestDto.CreateHealthRecordRequest request) {
        log.info("건강 정보 등록: 아이ID={}, 제목={}", request.getChildId(), request.getTitle());
        
        try {
            HealthResponseDto.HealthRecordResponse record = healthService.createHealthRecord(request);
            return ResponseEntity.ok(record);
        } catch (CareServiceException e) {
            log.error("건강 정보 등록 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 건강 정보 조회
     */
    @GetMapping("/records/{recordId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 정보 조회", description = "특정 건강 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "건강 정보를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<HealthResponseDto.HealthRecordResponse> getHealthRecord(
            @Parameter(description = "건강 정보 ID", required = true) @PathVariable Long recordId) {
        log.info("건강 정보 조회: 기록ID={}", recordId);
        
        try {
            HealthResponseDto.HealthRecordResponse record = healthService.getHealthRecordById(recordId);
            return ResponseEntity.ok(record);
        } catch (CareServiceException e) {
            log.error("건강 정보 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 사용자별 건강 정보 조회
     */
    @GetMapping("/records/user/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자별 건강 정보 조회", description = "특정 사용자의 모든 건강 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<HealthResponseDto.HealthRecordResponse>> getUserHealthRecords(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        log.info("사용자별 건강 정보 조회: 사용자ID={}", userId);
        
        try {
            List<HealthResponseDto.HealthRecordResponse> records = healthService.getHealthRecordsByUserId(userId);
            return ResponseEntity.ok(records);
        } catch (CareServiceException e) {
            log.error("사용자별 건강 정보 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 건강 정보 수정
     */
    @PutMapping("/records/{recordId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 정보 수정", description = "기존 건강 정보를 수정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "건강 정보를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<HealthResponseDto.HealthRecordResponse> updateHealthRecord(
            @Parameter(description = "건강 정보 ID", required = true) @PathVariable Long recordId,
            @Parameter(description = "수정할 건강 정보", required = true) 
            @RequestBody HealthRequestDto.UpdateHealthRecordRequest request) {
        log.info("건강 정보 수정: 기록ID={}", recordId);
        
        try {
            HealthResponseDto.HealthRecordResponse record = healthService.updateHealthRecord(recordId, request);
            return ResponseEntity.ok(record);
        } catch (CareServiceException e) {
            log.error("건강 정보 수정 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 건강 정보 삭제
     */
    @DeleteMapping("/records/{recordId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 정보 삭제", description = "건강 정보를 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "건강 정보를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> deleteHealthRecord(
            @Parameter(description = "건강 정보 ID", required = true) @PathVariable Long recordId) {
        log.info("건강 정보 삭제: 기록ID={}", recordId);
        
        try {
            healthService.deleteHealthRecord(recordId);
            return ResponseEntity.ok().build();
        } catch (CareServiceException e) {
            log.error("건강 정보 삭제 오류: {}", e.getMessage());
            throw e;
        }
    }

    // ==================== 건강 통계 ====================

    /**
     * 건강 통계 조회
     */
    @GetMapping("/statistics")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 통계 조회", description = "사용자의 건강 관련 통계를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<HealthResponseDto.HealthStatsResponse> getHealthStatistics(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {
        log.info("건강 통계 조회: 사용자ID={}", userId);
        
        try {
            HealthResponseDto.HealthStatsResponse statistics = healthService.getHealthStatistics(userId);
            return ResponseEntity.ok(statistics);
        } catch (CareServiceException e) {
            log.error("건강 통계 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 예방접종 스케줄 조회
     */
    @GetMapping("/vaccines/schedule")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "예방접종 스케줄 조회", description = "아동의 예방접종 스케줄을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<HealthResponseDto.VaccineScheduleResponse>> getVaccineSchedule(
            @Parameter(description = "아동 ID", required = true) @RequestParam String childId) {
        log.info("예방접종 스케줄 조회: 아동ID={}", childId);
        
        try {
            List<HealthResponseDto.VaccineScheduleResponse> schedule = healthService.getVaccineSchedule(childId);
            return ResponseEntity.ok(schedule);
        } catch (CareServiceException e) {
            log.error("예방접종 스케줄 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 건강 검진 스케줄 조회
     */
    @GetMapping("/checkups/schedule")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 검진 스케줄 조회", description = "아동의 건강 검진 스케줄을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<HealthResponseDto.CheckupScheduleResponse>> getCheckupSchedule(
            @Parameter(description = "아동 ID", required = true) @RequestParam String childId) {
        log.info("건강 검진 스케줄 조회: 아동ID={}", childId);
        
        try {
            List<HealthResponseDto.CheckupScheduleResponse> schedule = healthService.getCheckupSchedule(childId);
            return ResponseEntity.ok(schedule);
        } catch (CareServiceException e) {
            log.error("건강 검진 스케줄 조회 오류: {}", e.getMessage());
            throw e;
        }
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
        log.info("건강 알림 조회: 사용자ID={}", userId);
        
        try {
            List<HealthResponseDto.HealthAlertResponse> alerts = healthService.getHealthAlerts(userId);
            return ResponseEntity.ok(alerts);
        } catch (CareServiceException e) {
            log.error("건강 알림 조회 오류: {}", e.getMessage());
            throw e;
        }
    }


}
