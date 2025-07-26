package com.carecode.domain.health.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.health.dto.HealthRequestDto;
import com.carecode.domain.health.dto.HealthResponseDto;
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
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.health.dto.HealthRequestDto;
import com.carecode.domain.health.dto.HealthResponseDto;
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
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 건강 관리 API 컨트롤러
 * 육아 관련 건강 정보 관리 서비스
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "건강 관리", description = "육아 관련 건강 정보 관리 API")
public class HealthCheckController extends BaseController {

    private final HealthService healthService;

    /**
     * 건강 정보 등록
     */
    @PostMapping("/records")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 정보 등록", description = "새로운 건강 정보를 등록합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "등록 성공",
            content = @Content(schema = @Schema(implementation = HealthResponseDto.HealthRecordResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<HealthResponseDto.HealthRecordResponse> createHealthRecord(
            @Parameter(description = "건강 정보", required = true) @RequestBody HealthRequestDto.CreateHealthRecordRequest request) {
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
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = HealthResponseDto.HealthRecordResponse.class))),
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
    @Operation(summary = "사용자별 건강 정보 조회", description = "특정 사용자의 건강 정보 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = HealthResponseDto.HealthRecordResponse.class))),
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
        @ApiResponse(responseCode = "200", description = "수정 성공",
            content = @Content(schema = @Schema(implementation = HealthResponseDto.HealthRecordResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "건강 정보를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<HealthResponseDto.HealthRecordResponse> updateHealthRecord(
            @Parameter(description = "건강 정보 ID", required = true) @PathVariable Long recordId,
            @Parameter(description = "수정할 건강 정보", required = true) @RequestBody HealthRequestDto.UpdateHealthRecordRequest request) {
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
    public ResponseEntity<Map<String, String>> deleteHealthRecord(
            @Parameter(description = "건강 정보 ID", required = true) @PathVariable Long recordId) {
        log.info("건강 정보 삭제: 기록ID={}", recordId);
        
        try {
            healthService.deleteHealthRecord(recordId);
            return ResponseEntity.ok(Map.of("message", "건강 정보가 삭제되었습니다."));
        } catch (CareServiceException e) {
            log.error("건강 정보 삭제 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 건강 상태 분석
     */
    @PostMapping("/analysis")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 상태 분석", description = "사용자의 건강 상태를 분석합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "분석 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> analyzeHealthStatus(
            @Parameter(description = "분석 요청 정보", required = true) @RequestBody HealthRequestDto.CreateHealthRecordRequest request) {
        log.info("건강 상태 분석: 아이ID={}", request.getChildId());
        
        try {
            Map<String, Object> analysis = healthService.analyzeHealthStatus(request);
            return ResponseEntity.ok(analysis);
        } catch (CareServiceException e) {
            log.error("건강 상태 분석 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 건강 알림 설정
     */
    @PostMapping("/alerts")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 알림 설정", description = "건강 관련 알림을 설정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "설정 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, String>> setHealthAlert(
            @Parameter(description = "알림 설정 정보", required = true) @RequestBody HealthRequestDto.CreateHealthRecordRequest request) {
        log.info("건강 알림 설정: 아이ID={}", request.getChildId());
        
        try {
            healthService.setHealthAlert(request);
            return ResponseEntity.ok(Map.of("message", "건강 알림이 설정되었습니다."));
        } catch (CareServiceException e) {
            log.error("건강 알림 설정 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 건강 통계 조회
     */
    @GetMapping("/statistics/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 통계 조회", description = "사용자의 건강 관련 통계를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getHealthStatistics(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        log.info("건강 통계 조회: 사용자ID={}", userId);
        
        try {
            Map<String, Object> statistics = healthService.getHealthStatistics(userId);
            return ResponseEntity.ok(statistics);
        } catch (CareServiceException e) {
            log.error("건강 통계 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 건강 리포트 생성
     */
    @PostMapping("/reports")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 리포트 생성", description = "건강 관련 리포트를 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "생성 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> generateHealthReport(
            @Parameter(description = "리포트 생성 요청", required = true) @RequestBody HealthRequestDto.CreateHealthRecordRequest request) {
        log.info("건강 리포트 생성: 아이ID={}", request.getChildId());
        
        try {
            Map<String, Object> report = healthService.generateHealthReport(request);
            return ResponseEntity.ok(report);
        } catch (CareServiceException e) {
            log.error("건강 리포트 생성 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 건강 목표 설정
     */
    @PostMapping("/goals")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 목표 설정", description = "건강 관련 목표를 설정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "설정 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, String>> setHealthGoal(
            @Parameter(description = "건강 목표 정보", required = true) @RequestBody HealthRequestDto.CreateHealthRecordRequest request) {
        log.info("건강 목표 설정: 아이ID={}", request.getChildId());
        
        try {
            healthService.setHealthGoal(request);
            return ResponseEntity.ok(Map.of("message", "건강 목표가 설정되었습니다."));
        } catch (CareServiceException e) {
            log.error("건강 목표 설정 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 건강 목표 조회
     */
    @GetMapping("/goals/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "건강 목표 조회", description = "사용자의 건강 목표를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getHealthGoals(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        log.info("건강 목표 조회: 사용자ID={}", userId);
        
        try {
            Map<String, Object> goals = healthService.getHealthGoals(userId);
            return ResponseEntity.ok(goals);
        } catch (CareServiceException e) {
            log.error("건강 목표 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/records/user/{userId}/chart")
    public List<Map<String, Object>> getHealthChart(
            @PathVariable String userId,
            @RequestParam String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return healthService.getHealthChart(userId, type, from, to);
    }
} 