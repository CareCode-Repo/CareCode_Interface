package com.carecode.domain.careFacility.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.annotation.ValidateLocation;
import com.carecode.core.annotation.ValidateChildAge;
import com.carecode.core.controller.BaseController;
import com.carecode.domain.careFacility.dto.CareFacilityDto;
import com.carecode.domain.careFacility.dto.CareFacilitySearchRequestDto;
import com.carecode.domain.careFacility.dto.CareFacilitySearchResponseDto;
import com.carecode.domain.careFacility.service.CareFacilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 육아 시설 컨트롤러
 * 육아 시설 관련 REST API 엔드포인트 제공
 */
@Slf4j
@RestController
@RequestMapping("/facilities")
@RequiredArgsConstructor
public class CareFacilityController extends BaseController {
    
    private final CareFacilityService careFacilityService;
    
    /**
     * 전체 시설 목록 조회
     */
    @GetMapping
    @LogExecutionTime
    public ResponseEntity<List<CareFacilityDto>> getAllFacilities() {
        log.info("전체 시설 목록 조회 요청");
        List<CareFacilityDto> facilities = careFacilityService.getAllCareFacilities();
        return ResponseEntity.ok(facilities);
    }
    
    /**
     * 시설 ID로 시설 조회
     */
    @GetMapping("/{id}")
    @LogExecutionTime
    public ResponseEntity<CareFacilityDto> getFacilityById(@PathVariable Long id) {
        log.info("시설 조회 요청 - ID: {}", id);
        CareFacilityDto facility = careFacilityService.getCareFacilityById(id);
        return ResponseEntity.ok(facility);
    }
    
    /**
     * 시설 유형별 조회
     */
    @GetMapping("/type/{facilityType}")
    @LogExecutionTime
    public ResponseEntity<List<CareFacilityDto>> getFacilitiesByType(@PathVariable String facilityType) {
        log.info("시설 유형별 조회 요청 - 유형: {}", facilityType);
        List<CareFacilityDto> facilities = careFacilityService.getCareFacilitiesByType(facilityType);
        return ResponseEntity.ok(facilities);
    }
    
    /**
     * 지역별 시설 조회
     */
    @GetMapping("/location/{location}")
    @LogExecutionTime
    @ValidateLocation
    public ResponseEntity<List<CareFacilityDto>> getFacilitiesByLocation(@PathVariable String location) {
        log.info("지역별 시설 조회 요청 - 지역: {}", location);
        List<CareFacilityDto> facilities = careFacilityService.getCareFacilitiesByLocation(location);
        return ResponseEntity.ok(facilities);
    }
    
    /**
     * 연령대별 시설 조회
     */
    @GetMapping("/age")
    @LogExecutionTime
    @ValidateChildAge
    public ResponseEntity<List<CareFacilityDto>> getFacilitiesByAgeRange(
            @RequestParam Integer minAge, 
            @RequestParam Integer maxAge) {
        log.info("연령대별 시설 조회 요청 - 최소연령: {}, 최대연령: {}", minAge, maxAge);
        List<CareFacilityDto> facilities = careFacilityService.getCareFacilitiesByAgeRange(minAge, maxAge);
        return ResponseEntity.ok(facilities);
    }
    
    /**
     * 운영 시간별 시설 조회
     */
    @GetMapping("/operating-hours")
    @LogExecutionTime
    public ResponseEntity<List<CareFacilityDto>> getFacilitiesByOperatingHours(@RequestParam String operatingHours) {
        log.info("운영 시간별 시설 조회 요청 - 운영시간: {}", operatingHours);
        List<CareFacilityDto> facilities = careFacilityService.getCareFacilitiesByOperatingHours(operatingHours);
        return ResponseEntity.ok(facilities);
    }
    
    /**
     * 인기 시설 조회 (평점 기준)
     */
    @GetMapping("/popular")
    @LogExecutionTime
    public ResponseEntity<List<CareFacilityDto>> getPopularFacilities(@RequestParam(defaultValue = "10") Integer limit) {
        log.info("인기 시설 조회 요청 - 제한: {}", limit);
        List<CareFacilityDto> facilities = careFacilityService.getPopularCareFacilities(limit);
        return ResponseEntity.ok(facilities);
    }
    
    /**
     * 신규 시설 조회
     */
    @GetMapping("/new")
    @LogExecutionTime
    public ResponseEntity<List<CareFacilityDto>> getNewFacilities(@RequestParam(defaultValue = "10") Integer limit) {
        log.info("신규 시설 조회 요청 - 제한: {}", limit);
        List<CareFacilityDto> facilities = careFacilityService.getNewCareFacilities(limit);
        return ResponseEntity.ok(facilities);
    }
    
    /**
     * 위치 기반 시설 검색 (반경 내)
     */
    @GetMapping("/radius")
    @LogExecutionTime
    @ValidateLocation
    public ResponseEntity<List<CareFacilityDto>> getFacilitiesWithinRadius(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radius) {
        log.info("반경 내 시설 검색 요청 - 위도: {}, 경도: {}, 반경: {}", latitude, longitude, radius);
        List<CareFacilityDto> facilities = careFacilityService.getCareFacilitiesWithinRadius(latitude, longitude, radius);
        return ResponseEntity.ok(facilities);
    }
    
    /**
     * 복합 조건으로 시설 검색 (페이징)
     */
    @PostMapping("/search")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<CareFacilitySearchResponseDto> searchFacilities(@RequestBody CareFacilitySearchRequestDto requestDto) {
        log.info("복합 조건 시설 검색 요청 - {}", requestDto);
        CareFacilitySearchResponseDto response = careFacilityService.searchCareFacilities(requestDto);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 시설 조회수 증가
     */
    @PostMapping("/{id}/view")
    @LogExecutionTime
    public ResponseEntity<Map<String, String>> incrementViewCount(@PathVariable Long id) {
        log.info("시설 조회수 증가 요청 - ID: {}", id);
        careFacilityService.incrementViewCount(id);
        return ResponseEntity.ok(Map.of("message", SUCCESS_MESSAGE));
    }
    
    /**
     * 시설 평점 업데이트
     */
    @PostMapping("/{id}/rating")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<Map<String, String>> updateRating(
            @PathVariable Long id,
            @RequestParam Double rating) {
        log.info("시설 평점 업데이트 요청 - ID: {}, 평점: {}", id, rating);
        careFacilityService.updateRating(id, rating);
        return ResponseEntity.ok(Map.of("message", SUCCESS_MESSAGE));
    }
    
    /**
     * 시설 통계 조회
     */
    @GetMapping("/statistics")
    @LogExecutionTime
    public ResponseEntity<CareFacilitySearchResponseDto.FacilityStats> getFacilityStatistics() {
        log.info("시설 통계 조회 요청");
        CareFacilitySearchResponseDto.FacilityStats stats = careFacilityService.getFacilityStats();
        return ResponseEntity.ok(stats);
    }
} 