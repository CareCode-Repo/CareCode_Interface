package com.carecode.domain.careFacility.service;

import com.carecode.core.annotation.CacheableResult;
import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.ValidateLocation;
import com.carecode.core.exception.CareFacilityNotFoundException;
import com.carecode.domain.careFacility.dto.request.CareFacilityRequest;
import com.carecode.domain.careFacility.dto.request.CareFacilitySearchRequest;
import com.carecode.domain.careFacility.dto.response.CareFacilityInfo;
import com.carecode.domain.careFacility.dto.response.CareFacilityListResponse;
import com.carecode.domain.careFacility.dto.response.CareFacilityStatsResponse;
import com.carecode.domain.careFacility.dto.response.TypeStats;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.FacilityType;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.domain.careFacility.mapper.CareFacilityMapper;
import com.carecode.core.util.LocationUtil;
import com.carecode.core.util.CommonUtil;
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
    private final CareFacilityMapper careFacilityMapper;

    /**
     * 공공데이터 API에서 받아온 보육시설 데이터를 DB에 저장
     */
    @Transactional
    public void saveCareFacilitiesFromPublicData(Map<String, Object> publicData) {
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
                    
    }

    /**
     * 공공데이터로부터 새로운 CareFacility 엔티티 생성
     */
    private CareFacility createCareFacilityFromPublicData(Map<String, Object> facilityData) {
            return CareFacility.builder()
                    .facilityCode((String) facilityData.get("facilityId"))
                    .name((String) facilityData.get("facilityName"))
                    .facilityType(mapServiceTypeToFacilityType((String) facilityData.get("serviceType")))
                    .city("서울특별시") // 서울시 API이므로 고정
                    .district((String) facilityData.get("district"))
                    .address((String) facilityData.get("address"))
                    .latitude(CommonUtil.parseDouble(facilityData.get("latitude")))
                    .longitude(CommonUtil.parseDouble(facilityData.get("longitude")))
                    .website((String) facilityData.get("websiteUrl"))
                    .ageRange((String) facilityData.get("ageGroup"))
                    .operatingHours(formatOperatingHours(facilityData))
                    .tuitionFee(CommonUtil.parseInteger(facilityData.get("rentFee")))
                    .isActive(true)
                    .isPublic(true) // 공공데이터는 주로 공립 시설
                    .subsidyAvailable((Boolean) facilityData.get("isFree"))
                    .description(generateDescription(facilityData))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
                    
    }

    /**
     * 기존 CareFacility 엔티티를 공공데이터로 업데이트
     */
    private void updateCareFacilityFromPublicData(CareFacility existingFacility, Map<String, Object> facilityData) {
            existingFacility.setName((String) facilityData.get("facilityName"));
            existingFacility.setFacilityType(mapServiceTypeToFacilityType((String) facilityData.get("serviceType")));
            existingFacility.setDistrict((String) facilityData.get("district"));
            existingFacility.setAddress((String) facilityData.get("address"));
            existingFacility.setLatitude(CommonUtil.parseDouble(facilityData.get("latitude")));
            existingFacility.setLongitude(CommonUtil.parseDouble(facilityData.get("longitude")));
            existingFacility.setWebsite((String) facilityData.get("websiteUrl"));
            existingFacility.setAgeRange((String) facilityData.get("ageGroup"));
            existingFacility.setOperatingHours(formatOperatingHours(facilityData));
            existingFacility.setTuitionFee(CommonUtil.parseInteger(facilityData.get("rentFee")));
            existingFacility.setSubsidyAvailable((Boolean) facilityData.get("isFree"));
            existingFacility.setDescription(generateDescription(facilityData));
            existingFacility.setUpdatedAt(LocalDateTime.now());
            
            careFacilityRepository.save(existingFacility);
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
     * 돌봄 시설 목록 조회
     */
    @LogExecutionTime
    public List<CareFacilityInfo> getAllCareFacilities() {

        List<CareFacility> facilities = careFacilityRepository.findAll();
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 돌봄 시설 상세 조회
     */
    @LogExecutionTime
    @CacheableResult(cacheName = "careFacility", key = "#facilityId")
    public CareFacilityInfo getCareFacilityById(Long facilityId) {
        CareFacility facility = careFacilityRepository.findById(facilityId)
                .orElseThrow(() -> new CareFacilityNotFoundException("돌봄 시설을 찾을 수 없습니다: " + facilityId));
        
        return careFacilityMapper.toResponse(facility);
    }

    /**
     * 돌봄 시설 검색
     */
    @LogExecutionTime
    @ValidateLocation
    public CareFacilityListResponse searchCareFacilities(CareFacilitySearchRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.ASC, "name")
        );
        
        Page<CareFacility> facilityPage = careFacilityRepository.findBySearchCriteria(
                request.getKeyword(),
                null,
                request.getCity(),
                pageable
        );
        
        List<CareFacilityInfo> facilities = facilityPage.getContent().stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
        
        return CareFacilityListResponse.builder()
                .facilities(facilities)
                .totalCount(facilityPage.getTotalElements())
                .currentPage(facilityPage.getNumber())
                .totalPages(facilityPage.getTotalPages())
                .hasNext(facilityPage.hasNext())
                .hasPrevious(facilityPage.hasPrevious())
                .build();
    }

    /**
     * 시설 유형별 조회
     */
    @LogExecutionTime
    public List<CareFacilityInfo> getCareFacilitiesByType(FacilityType facilityType) {
        List<CareFacility> facilities = careFacilityRepository.findByFacilityType(facilityType);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 지역별 돌봄 시설 조회
     */
    @LogExecutionTime
    @ValidateLocation
    public List<CareFacilityInfo> getCareFacilitiesByLocation(String location) {
        List<CareFacility> facilities = careFacilityRepository.findByAddressContaining(location);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 반경 내 돌봄 시설 조회
     */
    @LogExecutionTime
    @ValidateLocation
    public List<CareFacilityInfo> getCareFacilitiesWithinRadius(Double latitude, Double longitude, Double radius) {
        List<CareFacility> facilities = careFacilityRepository.findWithinRadius(latitude, longitude, radius);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 연령대별 돌봄 시설 조회
     */
    @LogExecutionTime
    public List<CareFacilityInfo> getCareFacilitiesByAgeRange(int minAge, int maxAge) {
        List<CareFacility> facilities = careFacilityRepository.findByAgeRange(minAge, maxAge);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 운영 시간별 돌봄 시설 조회
     */
    @LogExecutionTime
    public List<CareFacilityInfo> getCareFacilitiesByOperatingHours(String operatingHours) {
        List<CareFacility> facilities = careFacilityRepository.findByOperatingHours(operatingHours);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 인기 돌봄 시설 조회 (평점 기준)
     */
    @LogExecutionTime
    public List<CareFacilityInfo> getPopularCareFacilities(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<CareFacility> facilities = careFacilityRepository.findPopularFacilities(pageable);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 신규 돌봄 시설 조회
     */
    @LogExecutionTime
    public List<CareFacilityInfo> getNewCareFacilities(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<CareFacility> facilities = careFacilityRepository.findNewFacilities(pageable);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 돌봄 시설 조회수 증가
     */
    @Transactional
    public void incrementViewCount(Long facilityId) {
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
    public CareFacilityStatsResponse getFacilityStats() {
        long totalFacilities = careFacilityRepository.count();
        long totalViews = careFacilityRepository.getTotalViewCount();
        List<TypeStats> typeStats = careFacilityRepository.getTypeStats();
        
        return CareFacilityStatsResponse.builder()
                .totalFacilities(totalFacilities)
                .totalBookings(0L)
                .activeFacilities(0L)
                .typeDistribution(null)
                .typeStats(null)
                .todayBookings(0L)
                .thisWeekBookings(0L)
                .thisMonthBookings(0L)
                .build();
    }

    /**
     * Entity를 DTO로 변환
     */
    // 매핑은 CareFacilityMapper 사용
} 