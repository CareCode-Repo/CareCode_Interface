package com.carecode.domain.policy.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.ValidateLocation;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.CareServiceException;
import com.carecode.core.exception.PolicyNotFoundException;
import com.carecode.domain.policy.dto.response.PolicyDto;
import com.carecode.domain.policy.dto.request.PolicyRequest;
import com.carecode.domain.policy.dto.request.PolicySearchRequest;
import com.carecode.domain.policy.dto.response.PolicyResponse;
import com.carecode.domain.policy.dto.response.PolicyInfoResponse;
import com.carecode.domain.policy.dto.response.PolicyListResponse;
import com.carecode.domain.policy.dto.response.PolicyStatsSimpleResponse;
import com.carecode.domain.policy.app.PolicyFacade;
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

import java.util.List;
import com.carecode.core.handler.ApiSuccess;
import java.util.Date;

/**
 * 육아 정책 API 컨트롤러
 * 정부 육아 정책 정보 제공 및 검색 서비스
 */
@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*") // CORS 허용
@Tag(name = "육아 정책", description = "육아 정책 정보 및 검색 API")
public class PolicyController extends BaseController {

    private final PolicyFacade policyFacade;

    /**
     * 전체 정책 목록 조회
     */
    @GetMapping
    @LogExecutionTime
    @Operation(summary = "전체 정책 목록 조회", description = "등록된 모든 육아 정책 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = PolicyInfoResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<PolicyDto>> getAllPolicies() {
        log.info("전체 정책 목록 조회");
        List<PolicyDto> policies = policyFacade.getAllPolicies();
        return ResponseEntity.ok(policies);
    }

    /**
     * 정책 상세 정보 조회
     */
    @GetMapping("/{policyId}")
    @LogExecutionTime
    @Operation(summary = "정책 상세 조회", description = "정책 ID로 특정 육아 정책의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = PolicyInfoResponse.class))),
        @ApiResponse(responseCode = "404", description = "정책을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<PolicyDto> getPolicy(
            @Parameter(description = "정책 ID", required = true) @PathVariable Long policyId) {
        log.info("정책 상세 조회: 정책ID={}", policyId);

        try {
            PolicyDto policy = policyFacade.getPolicyById(policyId);
            return ResponseEntity.ok(policy);
        } catch (PolicyNotFoundException e) {
            log.error("정책을 찾을 수 없음: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 정책 검색 (페이징)
     */
    @PostMapping("/search")
    @LogExecutionTime
    @Operation(summary = "정책 검색", description = "다양한 조건으로 육아 정책을 검색합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "검색 성공",
            content = @Content(schema = @Schema(implementation = PolicyListResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<PolicyListResponse> searchPolicies(
            @Parameter(description = "검색 조건", required = true) @RequestBody PolicySearchRequest request) {
        log.info("정책 검색: 키워드={}, 카테고리={}, 지역={}",
                request.getKeyword(), request.getCategory(), request.getLocation());

        try {
            PolicyListResponse response = policyFacade.searchPolicies(request);
            return ResponseEntity.ok(response);
        } catch (CareServiceException e) {
            log.error("정책 검색 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 카테고리별 정책 조회
     */
    @GetMapping("/category/{category}")
    @LogExecutionTime
    @Operation(summary = "카테고리별 정책 조회", description = "특정 카테고리의 육아 정책 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = PolicyInfoResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<PolicyDto>> getPoliciesByCategory(
            @Parameter(description = "정책 카테고리", required = true) @PathVariable String category) {
        log.info("카테고리별 정책 조회: 카테고리={}", category);

        try {
            List<PolicyDto> policies = policyFacade.getPoliciesByCategory(category);
            return ResponseEntity.ok(policies);
        } catch (CareServiceException e) {
            log.error("카테고리별 정책 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 지역별 정책 조회
     */
    @GetMapping("/location/{location}")
    @LogExecutionTime
    @ValidateLocation
    @Operation(summary = "지역별 정책 조회", description = "특정 지역의 육아 정책 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = PolicyInfoResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 지역 정보"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<PolicyDto>> getPoliciesByLocation(
            @Parameter(description = "지역명", required = true) @PathVariable String location) {
        log.info("지역별 정책 조회: 지역={}", location);

        try {
            List<PolicyDto> policies = policyFacade.getPoliciesByLocation(location);
            return ResponseEntity.ok(policies);
        } catch (CareServiceException e) {
            log.error("지역별 정책 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 연령대별 정책 조회
     */
    @GetMapping("/age")
    @LogExecutionTime
    @Operation(summary = "연령대별 정책 조회", description = "특정 연령대에 해당하는 육아 정책 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = PolicyInfoResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 연령 정보"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<PolicyDto>> getPoliciesByAgeRange(
            @Parameter(description = "최소 연령", required = true) @RequestParam Integer minAge,
            @Parameter(description = "최대 연령", required = true) @RequestParam Integer maxAge) {
        log.info("연령대별 정책 조회: 최소연령={}, 최대연령={}", minAge, maxAge);

        try {
            List<PolicyDto> policies = policyFacade.getPoliciesByAgeRange(minAge, maxAge);
            return ResponseEntity.ok(policies);
        } catch (CareServiceException e) {
            log.error("연령대별 정책 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 인기 정책 조회
     */
    @GetMapping("/popular")
    @LogExecutionTime
    @Operation(summary = "인기 정책 조회", description = "인기 있는 육아 정책 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = PolicyInfoResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<PolicyDto>> getPopularPolicies(
            @Parameter(description = "조회할 정책 수", example = "10") @RequestParam(defaultValue = "10") Integer limit) {
        log.info("인기 정책 조회: 제한={}", limit);

        try {
            List<PolicyDto> policies = policyFacade.getPopularPolicies(limit);
            return ResponseEntity.ok(policies);
        } catch (CareServiceException e) {
            log.error("인기 정책 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 최신 정책 조회
     */
    @GetMapping("/latest")
    @LogExecutionTime
    @Operation(summary = "최신 정책 조회", description = "최근 등록된 육아 정책 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = PolicyInfoResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<PolicyDto>> getLatestPolicies(
            @Parameter(description = "조회할 정책 수", example = "10") @RequestParam(defaultValue = "10") Integer limit) {
        log.info("최신 정책 조회: 제한={}", limit);

        try {
            List<PolicyDto> policies = policyFacade.getLatestPolicies(limit);
            return ResponseEntity.ok(policies);
        } catch (CareServiceException e) {
            log.error("최신 정책 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 정책 조회수 증가
     */
    @PostMapping("/{policyId}/view")
    @LogExecutionTime
    @Operation(summary = "정책 조회수 증가", description = "특정 정책의 조회수를 증가시킵니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회수 증가 성공"),
        @ApiResponse(responseCode = "404", description = "정책을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiSuccess> incrementViewCount(
            @Parameter(description = "정책 ID", required = true) @PathVariable Long policyId) {
        log.info("정책 조회수 증가: 정책ID={}", policyId);
        
        try {
            policyFacade.incrementViewCount(policyId);
            return ResponseEntity.ok(ApiSuccess.builder().timestamp(new Date()).message("조회수가 증가되었습니다.").build());
        } catch (PolicyNotFoundException e) {
            log.error("정책을 찾을 수 없음: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 정책 카테고리 목록 조회
     */
    @GetMapping("/categories")
    @LogExecutionTime
    @Operation(summary = "정책 카테고리 목록 조회", description = "사용 가능한 정책 카테고리 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "카테고리 목록 조회 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<String>> getPolicyCategories() {
        log.info("정책 카테고리 목록 조회");
        
        try {
            List<String> categories = policyFacade.getPolicyCategories();
            return ResponseEntity.ok(categories);
        } catch (CareServiceException e) {
            log.error("정책 카테고리 목록 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 정책 통계 조회
     */
    @GetMapping("/statistics")
    @LogExecutionTime
    @Operation(summary = "정책 통계 조회", description = "육아 정책 관련 통계 정보를 조회합니다.")
    public ResponseEntity<PolicyStatsSimpleResponse> getPolicyStatistics() {
        log.info("정책 통계 조회");
        
        try {
            PolicyStatsSimpleResponse stats = policyFacade.getPolicyStats();
            return ResponseEntity.ok(stats);
        } catch (CareServiceException e) {
            log.error("정책 통계 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 아이 연령별 정책 조회
     */
    @GetMapping("/child-age")
    @LogExecutionTime
    @Operation(summary = "아이 연령별 정책 조회", description = "특정 연령의 아이에게 해당하는 정책을 조회합니다.")
    public ResponseEntity<List<PolicyDto>> getPoliciesByChildAge(
            @Parameter(description = "아이 연령", required = true) @RequestParam Integer childAge) {
        log.info("아이 연령별 정책 조회: 아이 연령={}", childAge);
        
        try {
            List<PolicyDto> policies = policyFacade.getPoliciesByChildAge(childAge);
            return ResponseEntity.ok(policies);
        } catch (CareServiceException e) {
            log.error("아이 연령별 정책 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 신청 기간이 유효한 정책 조회
     */
    @GetMapping("/active")
    @LogExecutionTime
    @Operation(summary = "신청 기간이 유효한 정책 조회", description = "현재 신청 기간 내에 있는 정책을 조회합니다.")
    public ResponseEntity<List<PolicyDto>> getActivePoliciesByDate() {
        log.info("신청 기간이 유효한 정책 조회");
        
        try {
            List<PolicyDto> policies = policyFacade.getActivePoliciesByDate();
            return ResponseEntity.ok(policies);
        } catch (CareServiceException e) {
            log.error("신청 기간이 유효한 정책 조회 오류: {}", e.getMessage());
            throw e;
        }
    }
} 