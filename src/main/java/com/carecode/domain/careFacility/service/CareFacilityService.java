package com.carecode.domain.careFacility.service;

import com.carecode.core.annotation.CacheableResult;
import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.ValidateLocation;
import com.carecode.core.exception.CareFacilityNotFoundException;
import com.carecode.domain.careFacility.dto.CareFacilityDto;
import com.carecode.domain.careFacility.dto.CareFacilitySearchRequestDto;
import com.carecode.domain.careFacility.dto.CareFacilitySearchResponseDto;
import com.carecode.domain.careFacility.dto.TypeStats;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.core.util.LocationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 돌봄 시설 서비스 클래스
 * 육아 지원 시설 관련 비즈니스 로직 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CareFacilityService {

    private final CareFacilityRepository careFacilityRepository;

    /**
     * 돌봄 시설 목록 조회
     */
    @LogExecutionTime
    public List<CareFacilityDto> getAllCareFacilities() {
        log.info("전체 돌봄 시설 목록 조회");
        
        List<CareFacility> facilities = careFacilityRepository.findAll();
        return facilities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 돌봄 시설 상세 조회
     */
    @LogExecutionTime
    @CacheableResult(cacheName = "careFacility", key = "#facilityId")
    public CareFacilityDto getCareFacilityById(Long facilityId) {
        log.info("돌봄 시설 상세 조회: 시설ID={}", facilityId);
        
        CareFacility facility = careFacilityRepository.findById(facilityId)
                .orElseThrow(() -> new CareFacilityNotFoundException("돌봄 시설을 찾을 수 없습니다: " + facilityId));
        
        return convertToDto(facility);
    }

    /**
     * 돌봄 시설 검색
     */
    @LogExecutionTime
    @ValidateLocation
    public CareFacilitySearchResponseDto searchCareFacilities(CareFacilitySearchRequestDto request) {
        log.info("돌봄 시설 검색: 키워드={}, 시설유형={}, 지역={}", 
                request.getKeyword(), request.getFacilityType(), request.getRegion());
        
        Pageable pageable = PageRequest.of(
                request.getPage() != null ? request.getPage() : 0, 
                request.getSize() != null ? request.getSize() : 10, 
                Sort.by(Sort.Direction.ASC, "name")
        );
        
        Page<CareFacility> facilityPage = careFacilityRepository.findBySearchCriteria(
                request.getKeyword(),
                request.getFacilityType(),
                request.getRegion(),
                pageable
        );
        
        List<CareFacilityDto> facilities = facilityPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        return CareFacilitySearchResponseDto.builder()
                .facilities(facilities)
                .totalElements(facilityPage.getTotalElements())
                .totalPages(facilityPage.getTotalPages())
                .currentPage(facilityPage.getNumber())
                .pageSize(facilityPage.getSize())
                .hasNext(facilityPage.hasNext())
                .hasPrevious(facilityPage.hasPrevious())
                .build();
    }

    /**
     * 시설 유형별 조회
     */
    @LogExecutionTime
    public List<CareFacilityDto> getCareFacilitiesByType(String facilityType) {
        log.info("시설 유형별 조회: 유형={}", facilityType);
        
        List<CareFacility> facilities = careFacilityRepository.findByFacilityType(facilityType);
        return facilities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 지역별 돌봄 시설 조회
     */
    @LogExecutionTime
    @ValidateLocation
    public List<CareFacilityDto> getCareFacilitiesByLocation(String location) {
        log.info("지역별 돌봄 시설 조회: 지역={}", location);
        
        List<CareFacility> facilities = careFacilityRepository.findByAddressContaining(location);
        return facilities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 반경 내 돌봄 시설 조회
     */
    @LogExecutionTime
    @ValidateLocation
    public List<CareFacilityDto> getCareFacilitiesWithinRadius(
            Double latitude, Double longitude, Double radius) {
        log.info("반경 내 돌봄 시설 조회: 위도={}, 경도={}, 반경={}", latitude, longitude, radius);
        
        List<CareFacility> facilities = careFacilityRepository.findWithinRadius(latitude, longitude, radius);
        return facilities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 연령대별 돌봄 시설 조회
     */
    @LogExecutionTime
    public List<CareFacilityDto> getCareFacilitiesByAgeRange(int minAge, int maxAge) {
        log.info("연령대별 돌봄 시설 조회: 최소연령={}, 최대연령={}", minAge, maxAge);
        
        List<CareFacility> facilities = careFacilityRepository.findByAgeRange(minAge, maxAge);
        return facilities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 운영 시간별 돌봄 시설 조회
     */
    @LogExecutionTime
    public List<CareFacilityDto> getCareFacilitiesByOperatingHours(String operatingHours) {
        log.info("운영 시간별 돌봄 시설 조회: 운영시간={}", operatingHours);
        
        List<CareFacility> facilities = careFacilityRepository.findByOperatingHours(operatingHours);
        return facilities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 인기 돌봄 시설 조회 (평점 기준)
     */
    @LogExecutionTime
    public List<CareFacilityDto> getPopularCareFacilities(int limit) {
        log.info("인기 돌봄 시설 조회: 제한={}", limit);
        
        Pageable pageable = PageRequest.of(0, limit);
        List<CareFacility> facilities = careFacilityRepository.findPopularFacilities(pageable);
        return facilities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 신규 돌봄 시설 조회
     */
    @LogExecutionTime
    public List<CareFacilityDto> getNewCareFacilities(int limit) {
        log.info("신규 돌봄 시설 조회: 제한={}", limit);
        
        Pageable pageable = PageRequest.of(0, limit);
        List<CareFacility> facilities = careFacilityRepository.findNewFacilities(pageable);
        return facilities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 돌봄 시설 조회수 증가 (현재는 구현하지 않음 - viewCount 필드가 없음)
     */
    @Transactional
    public void incrementViewCount(Long facilityId) {
        log.info("돌봄 시설 조회수 증가: 시설ID={}", facilityId);
        
        // viewCount 필드가 엔티티에 없으므로 현재는 로그만 출력
        // 추후 viewCount 필드 추가 시 구현
        log.warn("viewCount 필드가 엔티티에 없어 조회수 증가 기능이 구현되지 않았습니다.");
    }

    /**
     * 돌봄 시설 평점 업데이트
     */
    @Transactional
    public void updateRating(Long facilityId, Double rating) {
        log.info("돌봄 시설 평점 업데이트: 시설ID={}, 평점={}", facilityId, rating);
        
        CareFacility facility = careFacilityRepository.findById(facilityId)
                .orElseThrow(() -> new CareFacilityNotFoundException("돌봄 시설을 찾을 수 없습니다: " + facilityId));
        
        // 평점 계산 로직 (기존 평점과 새로운 평점의 가중 평균)
        double currentRating = facility.getRating() != null ? facility.getRating() : 0.0;
        int reviewCount = facility.getReviewCount() != null ? facility.getReviewCount() : 0;
        
        double newRating = ((currentRating * reviewCount) + rating) / (reviewCount + 1);
        
        facility.setRating(newRating);
        facility.setReviewCount(reviewCount + 1);
        careFacilityRepository.save(facility);
    }

    /**
     * 돌봄 시설 통계 조회
     */
    @LogExecutionTime
    public CareFacilitySearchResponseDto.FacilityStats getFacilityStats() {
        log.info("돌봄 시설 통계 조회");
        
        long totalFacilities = careFacilityRepository.count();
        long totalViews = careFacilityRepository.getTotalViewCount();
        List<TypeStats> typeStats = careFacilityRepository.getTypeStats();
        
        return CareFacilitySearchResponseDto.FacilityStats.builder()
                .totalFacilities(totalFacilities)
                .totalViews(totalViews)
                .typeStats(typeStats)
                .build();
    }

    /**
     * Entity를 DTO로 변환
     */
    private CareFacilityDto convertToDto(CareFacility facility) {
        return CareFacilityDto.builder()
                .id(facility.getId())
                .name(facility.getName())
                .facilityType(facility.getFacilityType())
                .description(null) // description 필드가 엔티티에 없으므로 null로 설정
                .address(facility.getAddress())
                .location(facility.getAddress())
                .latitude(facility.getLatitude())
                .longitude(facility.getLongitude())
                .phoneNumber(facility.getPhone())
                .email(facility.getEmail())
                .websiteUrl(facility.getWebsite())
                .operatingHours(facility.getOperatingHours())
                .capacity(facility.getCapacity())
                .currentEnrollment(facility.getCurrentEnrollment())
                .minAge(facility.getAgeRangeMin())
                .maxAge(facility.getAgeRangeMax())
                .rating(facility.getRating())
                .reviewCount(facility.getReviewCount())
                .viewCount(0)
                .isActive(facility.getIsActive())
                .createdAt(facility.getCreatedAt())
                .updatedAt(facility.getUpdatedAt())
                .build();
    }
} 