package com.carecode.core.client.controller;

import com.carecode.core.client.PublicDataApiService;
import com.carecode.core.client.dto.PublicDataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 공공데이터 API 컨트롤러
 * 외부에서 공공데이터 API를 호출할 수 있는 REST API 엔드포인트 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/public-data")
@RequiredArgsConstructor
@Tag(name = "공공데이터 API", description = "공공데이터 포털 API 호출")
public class PublicDataController {

    private final PublicDataApiService publicDataApiService;

    /**
     * 육아 시설 정보 조회
     */
    @GetMapping("/childcare-facilities")
    @Operation(summary = "육아 시설 정보 조회", description = "지역별 육아 관련 시설 정보를 조회합니다.")
    public ResponseEntity<PublicDataResponse<Object>> getChildcareFacilities(
            @Parameter(description = "지역명") @RequestParam(required = false) String region,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "페이지당 행 수") @RequestParam(defaultValue = "10") int numOfRows) {
        
        log.info("육아 시설 정보 조회 요청: region={}, pageNo={}, numOfRows={}", region, pageNo, numOfRows);
        
        try {
            PublicDataResponse<Object> response = publicDataApiService.getChildcareFacilities(region, pageNo, numOfRows);
            
            if (publicDataApiService.validateResponse(response)) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("육아 시설 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 보육 정책 정보 조회
     */
    @GetMapping("/childcare-policies")
    @Operation(summary = "보육 정책 정보 조회", description = "보육 관련 정책 정보를 조회합니다.")
    public ResponseEntity<PublicDataResponse<Object>> getChildcarePolicies(
            @Parameter(description = "정책 유형") @RequestParam(required = false) String policyType,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "페이지당 행 수") @RequestParam(defaultValue = "10") int numOfRows) {
        
        log.info("보육 정책 정보 조회 요청: policyType={}, pageNo={}, numOfRows={}", policyType, pageNo, numOfRows);
        
        try {
            PublicDataResponse<Object> response = publicDataApiService.getChildcarePolicies(policyType, pageNo, numOfRows);
            
            if (publicDataApiService.validateResponse(response)) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("보육 정책 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 소아과 병원 정보 조회
     */
    @GetMapping("/pediatric-hospitals")
    @Operation(summary = "소아과 병원 정보 조회", description = "지역별 소아과 병원 정보를 조회합니다.")
    public ResponseEntity<PublicDataResponse<Object>> getPediatricHospitals(
            @Parameter(description = "지역명") @RequestParam(required = false) String region,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "페이지당 행 수") @RequestParam(defaultValue = "10") int numOfRows) {
        
        log.info("소아과 병원 정보 조회 요청: region={}, pageNo={}, numOfRows={}", region, pageNo, numOfRows);
        
        try {
            PublicDataResponse<Object> response = publicDataApiService.getPediatricHospitals(region, pageNo, numOfRows);
            
            if (publicDataApiService.validateResponse(response)) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("소아과 병원 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 육아 지원금 정보 조회
     */
    @GetMapping("/childcare-subsidies")
    @Operation(summary = "육아 지원금 정보 조회", description = "지역별 육아 지원금 정보를 조회합니다.")
    public ResponseEntity<PublicDataResponse<Object>> getChildcareSubsidies(
            @Parameter(description = "지역명") @RequestParam(required = false) String region,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "페이지당 행 수") @RequestParam(defaultValue = "10") int numOfRows) {
        
        log.info("육아 지원금 정보 조회 요청: region={}, pageNo={}, numOfRows={}", region, pageNo, numOfRows);
        
        try {
            PublicDataResponse<Object> response = publicDataApiService.getChildcareSubsidies(region, pageNo, numOfRows);
            
            if (publicDataApiService.validateResponse(response)) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("육아 지원금 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 육아 교육 정보 조회
     */
    @GetMapping("/childcare-education")
    @Operation(summary = "육아 교육 정보 조회", description = "육아 관련 교육 정보를 조회합니다.")
    public ResponseEntity<PublicDataResponse<Object>> getChildcareEducation(
            @Parameter(description = "교육 유형") @RequestParam(required = false) String educationType,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "페이지당 행 수") @RequestParam(defaultValue = "10") int numOfRows) {
        
        log.info("육아 교육 정보 조회 요청: educationType={}, pageNo={}, numOfRows={}", educationType, pageNo, numOfRows);
        
        try {
            PublicDataResponse<Object> response = publicDataApiService.getChildcareEducation(educationType, pageNo, numOfRows);
            
            if (publicDataApiService.validateResponse(response)) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("육아 교육 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 커스텀 API 호출
     */
    @GetMapping("/custom/{endpoint}")
    @Operation(summary = "커스텀 API 호출", description = "사용자 정의 엔드포인트로 공공데이터 API를 호출합니다.")
    public ResponseEntity<PublicDataResponse<Object>> callCustomApi(
            @Parameter(description = "API 엔드포인트") @PathVariable String endpoint,
            @Parameter(description = "쿼리 파라미터") @RequestParam Map<String, String> params) {
        
        log.info("커스텀 API 호출 요청: endpoint={}, params={}", endpoint, params);
        
        try {
            PublicDataResponse<Object> response = publicDataApiService.callCustomApi("/" + endpoint, params);
            
            if (publicDataApiService.validateResponse(response)) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("커스텀 API 호출 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 