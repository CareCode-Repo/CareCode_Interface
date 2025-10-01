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
import com.carecode.domain.careFacility.entity.FacilityType;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
     * 공공데이터 API에서 받아온 보육시설 데이터를 DB에 저장
     */
    @Transactional
    public void saveCareFacilitiesFromPublicData(Map<String, Object> publicData) {
        try {
            log.info("공공데이터에서 보육시설 정보 DB 저장 시작");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> facilities = (List<Map<String, Object>>) publicData.get("facilities");
            
            if (facilities == null || facilities.isEmpty()) {
                log.warn("저장할 보육시설 데이터가 없습니다.");
                return;
            }
            
            int savedCount = 0;
            int updatedCount = 0;
            int errorCount = 0;
            
            for (Map<String, Object> facilityData : facilities) {
                try {
                    String facilityId = (String) facilityData.get("facilityId");
                    
                    // 기존 시설이 있는지 확인
                    Optional<CareFacility> existingFacilityOpt = careFacilityRepository.findByFacilityCode(facilityId);
                    
                    if (existingFacilityOpt.isPresent()) {
                        // 기존 시설 업데이트
                        CareFacility existingFacility = existingFacilityOpt.get();
                        updateCareFacilityFromPublicData(existingFacility, facilityData);
                        updatedCount++;
                        log.debug("보육시설 업데이트: {}", facilityId);
                    } else {
                        // 새 시설 생성
                        CareFacility newFacility = createCareFacilityFromPublicData(facilityData);
                        careFacilityRepository.save(newFacility);
                        savedCount++;
                        log.debug("보육시설 신규 저장: {}", facilityId);
                    }
                    
                } catch (Exception e) {
                    errorCount++;
                    log.error("보육시설 데이터 저장 중 오류 발생: {}", e.getMessage(), e);
                }
            }
            
            log.info("공공데이터 보육시설 저장 완료: 신규={}, 업데이트={}, 오류={}", 
                    savedCount, updatedCount, errorCount);
                    
        } catch (Exception e) {
            log.error("공공데이터 보육시설 저장 중 전체 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("공공데이터 보육시설 저장 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 공공데이터로부터 새로운 CareFacility 엔티티 생성
     */
    private CareFacility createCareFacilityFromPublicData(Map<String, Object> facilityData) {
        try {
            return CareFacility.builder()
                    .facilityCode((String) facilityData.get("facilityId"))
                    .name((String) facilityData.get("facilityName"))
                    .facilityType(mapServiceTypeToFacilityType((String) facilityData.get("serviceType")))
                    .city("서울특별시") // 서울시 API이므로 고정
                    .district((String) facilityData.get("district"))
                    .address((String) facilityData.get("address"))
                    .latitude(parseDouble(facilityData.get("latitude")))
                    .longitude(parseDouble(facilityData.get("longitude")))
                    .website((String) facilityData.get("websiteUrl"))
                    .ageRange((String) facilityData.get("ageGroup"))
                    .operatingHours(formatOperatingHours(facilityData))
                    .tuitionFee(parseInteger(facilityData.get("rentFee")))
                    .isActive(true)
                    .isPublic(true) // 공공데이터는 주로 공립 시설
                    .subsidyAvailable((Boolean) facilityData.get("isFree"))
                    .description(generateDescription(facilityData))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("CareFacility 엔티티 생성 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("CareFacility 엔티티 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 기존 CareFacility 엔티티를 공공데이터로 업데이트
     */
    private void updateCareFacilityFromPublicData(CareFacility existingFacility, Map<String, Object> facilityData) {
        try {
            existingFacility.setName((String) facilityData.get("facilityName"));
            existingFacility.setFacilityType(mapServiceTypeToFacilityType((String) facilityData.get("serviceType")));
            existingFacility.setDistrict((String) facilityData.get("district"));
            existingFacility.setAddress((String) facilityData.get("address"));
            existingFacility.setLatitude(parseDouble(facilityData.get("latitude")));
            existingFacility.setLongitude(parseDouble(facilityData.get("longitude")));
            existingFacility.setWebsite((String) facilityData.get("websiteUrl"));
            existingFacility.setAgeRange((String) facilityData.get("ageGroup"));
            existingFacility.setOperatingHours(formatOperatingHours(facilityData));
            existingFacility.setTuitionFee(parseInteger(facilityData.get("rentFee")));
            existingFacility.setSubsidyAvailable((Boolean) facilityData.get("isFree"));
            existingFacility.setDescription(generateDescription(facilityData));
            existingFacility.setUpdatedAt(LocalDateTime.now());
            
            careFacilityRepository.save(existingFacility);
            
        } catch (Exception e) {
            log.error("CareFacility 엔티티 업데이트 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("CareFacility 엔티티 업데이트 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 서비스 타입을 FacilityType으로 매핑
     */
    private FacilityType mapServiceTypeToFacilityType(String serviceType) {
        if (serviceType == null) {
            return FacilityType.OTHER;
        }
        
        switch (serviceType) {
            case "공동육아나눔터":
                return FacilityType.PLAYGROUP;
            case "어린이집":
                return FacilityType.DAYCARE;
            case "유치원":
                return FacilityType.KINDERGARTEN;
            case "방과후교실":
                return FacilityType.NURSERY;
            default:
                return FacilityType.OTHER;
        }
    }

    /**
     * 운영시간 정보 포맷팅
     */
    private String formatOperatingHours(Map<String, Object> facilityData) {
        StringBuilder hours = new StringBuilder();
        
        String weekdayStart = (String) facilityData.get("weekdayStartTime");
        String weekdayEnd = (String) facilityData.get("weekdayEndTime");
        String operationType = (String) facilityData.get("operationType");
        Boolean hasSaturday = (Boolean) facilityData.get("hasSaturdayOperation");
        String saturdayStart = (String) facilityData.get("saturdayStartTime");
        String saturdayEnd = (String) facilityData.get("saturdayEndTime");
        
        if (weekdayStart != null && weekdayEnd != null) {
            hours.append("평일: ").append(weekdayStart).append("~").append(weekdayEnd);
        }
        
        if (operationType != null && !operationType.isEmpty()) {
            hours.append(" (").append(operationType).append(")");
        }
        
        if (hasSaturday != null && hasSaturday && saturdayStart != null && saturdayEnd != null) {
            hours.append(", 토요일: ").append(saturdayStart).append("~").append(saturdayEnd);
        }
        
        return hours.toString();
    }

    /**
     * 시설 설명 생성
     */
    private String generateDescription(Map<String, Object> facilityData) {
        StringBuilder description = new StringBuilder();
        
        String serviceType = (String) facilityData.get("serviceType");
        String ageGroup = (String) facilityData.get("ageGroup");
        String district = (String) facilityData.get("district");
        Boolean isFree = (Boolean) facilityData.get("isFree");
        
        if (serviceType != null) {
            description.append(serviceType);
        }
        
        if (ageGroup != null) {
            description.append(" - ").append(ageGroup);
        }
        
        if (district != null) {
            description.append(" (").append(district).append(")");
        }
        
        if (isFree != null && isFree) {
            description.append(" - 무료 이용 가능");
        }
        
        return description.toString();
    }

    /**
     * Double 파싱 헬퍼 메서드
     */
    private Double parseDouble(Object value) {
        if (value == null || value.toString().trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Double 파싱 실패: {}", value);
            return null;
        }
    }

    /**
     * Integer 파싱 헬퍼 메서드
     */
    private Integer parseInteger(Object value) {
        if (value == null || value.toString().trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Integer 파싱 실패: {}", value);
            return null;
        }
    }

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
    public List<CareFacilityDto> getCareFacilitiesByType(FacilityType facilityType) {
        log.info("시설 유형별 조회: 유형={} ({})", facilityType, facilityType.getDisplayName());
        
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
     * 돌봄 시설 조회수 증가
     */
    @Transactional
    public void incrementViewCount(Long facilityId) {
        log.info("돌봄 시설 조회수 증가: 시설ID={}", facilityId);
        
        CareFacility facility = careFacilityRepository.findById(facilityId)
                .orElseThrow(() -> new CareFacilityNotFoundException("돌봄 시설을 찾을 수 없습니다: " + facilityId));
        
        Integer currentViewCount = facility.getViewCount() != null ? facility.getViewCount() : 0;
        facility.setViewCount(currentViewCount + 1);
        careFacilityRepository.save(facility);
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
                .description(null)
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
                .viewCount(facility.getViewCount())
                .isActive(facility.getIsActive())
                .createdAt(facility.getCreatedAt())
                .updatedAt(facility.getUpdatedAt())
                .build();
    }
} 