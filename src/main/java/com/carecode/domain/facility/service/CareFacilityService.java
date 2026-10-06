package com.carecode.domain.facility.service;

import org.springframework.cache.annotation.Cacheable;
import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.util.BoundingBox;
import com.carecode.core.search.FullTextSearchSupport;
import com.carecode.core.annotation.ValidateLocation;
import com.carecode.core.exception.CareFacilityNotFoundException;
import com.carecode.domain.facility.dto.request.CareFacilitySearchRequest;
import com.carecode.domain.facility.dto.request.ReviewRequest;
import com.carecode.domain.facility.dto.response.CareFacilityInfo;
import com.carecode.domain.facility.dto.response.CareFacilityListResponse;
import com.carecode.domain.facility.dto.response.ReviewResponse;
import com.carecode.domain.facility.dto.response.CareFacilityStatsResponse;
import com.carecode.domain.facility.dto.response.TypeStats;
import com.carecode.domain.facility.entity.CareFacility;
import com.carecode.domain.facility.entity.FacilityType;
import com.carecode.domain.facility.entity.Review;
import com.carecode.domain.facility.repository.CareFacilityRepository;
import com.carecode.domain.facility.repository.ReviewRepository;
import com.carecode.domain.facility.mapper.CareFacilityMapper;
import com.carecode.core.util.CommonUtil;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
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

/** 돌봄 시설 서비스 클래스 육아 지원 시설 관련 비즈니스 로직 처리 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CareFacilityService {

    private final CareFacilityRepository careFacilityRepository;
    private final com.carecode.domain.facility.repository.CareFacilityBookingRepository bookingRepository;
    private final com.carecode.core.ops.sync.SyncFreshnessService syncFreshnessService;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CareFacilityMapper careFacilityMapper;
    private final FullTextSearchSupport fullTextSearchSupport;

    // 돌봄 시설 목록 조회

    /** 시설 목록 조회. 테이블 전체를 메모리로 올리지 않도록 항상 페이지 단위로 읽는다. */
    @LogExecutionTime
    public List<CareFacilityInfo> getAllCareFacilities(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return careFacilityRepository.findAll(pageable).getContent().stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 돌봄 시설 상세 조회
    @LogExecutionTime
    @Cacheable(cacheNames = "careFacility", key = "#facilityId")
    public CareFacilityInfo getCareFacilityById(Long facilityId) {
        CareFacility facility = careFacilityRepository.findById(facilityId)
                .orElseThrow(() -> new CareFacilityNotFoundException("돌봄 시설을 찾을 수 없습니다: " + facilityId));
        
        return careFacilityMapper.toResponse(facility);
    }

    // 돌봄 시설 검색
    @LogExecutionTime
    @ValidateLocation
    public CareFacilityListResponse searchCareFacilities(CareFacilitySearchRequest request) {
        Sort sort = com.carecode.core.util.SortUtil.createSort(
                request.getSortBy(),
                request.getSortDirection(),
                "name",
                Sort.Direction.ASC
        );
        // size 는 primitive 라 빠지면 0 이 되고 PageRequest.of 가 예외를 낸다.
        int page = com.carecode.core.util.PageRequestUtil.normalizePage(request.getPage());
        int size = com.carecode.core.util.PageRequestUtil.normalizeSize(request.getSize() > 0 ? request.getSize() : null);
        Pageable pageable = PageRequest.of(page, size, sort);

        // 전에는 facilityType 을 받기만 하고 쿼리에 null 을 넘겨, 유형 필터를 골라도 전체가 나왔다.
        FacilityType facilityType = parseFacilityType(request.getFacilityType());
        String address = firstNonBlank(request.getCity(), request.getDistrict());

        // 키워드만 있는 검색은 전문 검색으로 처리한다. LIKE '%키워드%' 는 인덱스를 못 탄다.
        boolean keywordOnly = address == null && facilityType == null;
        Page<CareFacility> facilityPage;
        if (keywordOnly && fullTextSearchSupport.canUseFullText(request.getKeyword())) {
            String normalized = fullTextSearchSupport.normalize(request.getKeyword());
            facilityPage = careFacilityRepository.searchByFullText(normalized, PageRequest.of(page, size));
        } else {
            String keyword = request.getKeyword() == null || request.getKeyword().isBlank()
                    ? null : request.getKeyword().trim();
            facilityPage = careFacilityRepository.findBySearchCriteria(
                    keyword, facilityType, address, pageable);
        }

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

    private static FacilityType parseFacilityType(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        try {
            return FacilityType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.carecode.core.exception.BusinessException(
                    com.carecode.core.exception.ErrorCode.INVALID_INPUT, "알 수 없는 시설 유형입니다: " + raw);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    // 시설 유형별 조회
    @LogExecutionTime
    public List<CareFacilityInfo> getCareFacilitiesByType(FacilityType facilityType) {
        List<CareFacility> facilities = careFacilityRepository.findByFacilityType(facilityType);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 지역별 돌봄 시설 조회
    @LogExecutionTime
    @ValidateLocation
    public List<CareFacilityInfo> getCareFacilitiesByLocation(String location) {
        List<CareFacility> facilities = careFacilityRepository.findByAddressContaining(location);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 반경 내 돌봄 시설 조회
    @LogExecutionTime
    @ValidateLocation
    public List<CareFacilityInfo> getCareFacilitiesWithinRadius(Double latitude, Double longitude, Double radius) {
        BoundingBox box = BoundingBox.around(latitude, longitude, radius);
        List<CareFacility> facilities = careFacilityRepository.findWithinBoundingBox(
                latitude, longitude, radius,
                box.minLat(), box.maxLat(), box.minLng(), box.maxLng());
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 연령대별 돌봄 시설 조회
    @LogExecutionTime
    public List<CareFacilityInfo> getCareFacilitiesByAgeRange(int minAge, int maxAge) {
        List<CareFacility> facilities = careFacilityRepository.findByAgeRange(minAge, maxAge);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 운영 시간별 돌봄 시설 조회
    @LogExecutionTime
    public List<CareFacilityInfo> getCareFacilitiesByOperatingHours(String operatingHours) {
        List<CareFacility> facilities = careFacilityRepository.findByOperatingHours(operatingHours);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 인기 돌봄 시설 조회 (평점 기준)
    @LogExecutionTime
    public List<CareFacilityInfo> getPopularCareFacilities(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<CareFacility> facilities = careFacilityRepository.findPopularFacilities(pageable);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 신규 돌봄 시설 조회
    @LogExecutionTime
    public List<CareFacilityInfo> getNewCareFacilities(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<CareFacility> facilities = careFacilityRepository.findNewFacilities(pageable);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 돌봄 시설 조회수 증가
    @Transactional
    public void incrementViewCount(Long facilityId) {
        // DB 에서 원자적으로 증가시킨다. 갱신된 행이 없으면 존재하지 않는 시설이다.
        int updated = careFacilityRepository.incrementViewCount(facilityId);
        if (updated == 0) {
            throw new CareFacilityNotFoundException("돌봄 시설을 찾을 수 없습니다: " + facilityId);
        }
    }

    // 돌봄 시설 평점 업데이트
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

    // 돌봄 시설 통계 조회
    @LogExecutionTime
    public CareFacilityStatsResponse getFacilityStats() {
        // 예전에는 유형별 통계를 조회해 놓고 버린 뒤 null 을, 활성 시설 수와 예약 수는 0 을 넣었다.
        // 필드가 있으면 클라이언트는 값이 온다고 믿으므로(소개 사이트가 이 값을 그대로 보여 준다) 실제 값을 채운다.
        List<TypeStats> typeStats = careFacilityRepository.getTypeStats();
        java.util.Map<String, Long> typeDistribution = new java.util.LinkedHashMap<>();
        for (TypeStats stats : typeStats) {
            if (stats.getFacilityType() != null) {
                typeDistribution.put(stats.getFacilityType().name(), stats.getCount());
            }
        }

        return CareFacilityStatsResponse.builder()
                .totalFacilities(careFacilityRepository.count())
                .activeFacilities(careFacilityRepository.countByIsActiveTrue())
                .typeDistribution(typeDistribution)
                .typeStats(typeStats)
                .totalBookings(bookingRepository.count())
                .todayBookings(bookingRepository.countTodayBookings())
                .thisWeekBookings(bookingRepository.countThisWeekBookings())
                .thisMonthBookings(bookingRepository.countThisMonthBookings())
                // 시설 목록은 어린이집·유치원 두 동기화가 채운다. 둘 중 오래된 쪽이 이 화면의 기준이다.
                .dataUpdatedAt(syncFreshnessService.lastFreshAt(
                        com.carecode.core.ops.sync.SyncJob.CHILDCARE_FACILITIES,
                        com.carecode.core.ops.sync.SyncJob.KINDERGARTENS).orElse(null))
                .build();
    }

    // 아이 연령별 시설 추천
    @LogExecutionTime
    public List<CareFacilityInfo> recommendFacilitiesByChildAge(Integer childAge) {
        List<CareFacility> facilities = careFacilityRepository.findByChildAge(childAge);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 최소 평점 이상의 시설 조회
    @LogExecutionTime
    public List<CareFacilityInfo> getFacilitiesByMinRating(Double minRating) {
        List<CareFacility> facilities = careFacilityRepository.findByMinRating(minRating);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 빈 자리가 있는 시설 조회
    @LogExecutionTime
    public List<CareFacilityInfo> getFacilitiesWithAvailableSpots(Integer minSpots) {
        List<CareFacility> facilities = careFacilityRepository.findByAvailableSpots(minSpots);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 등록금 범위로 시설 조회
    @LogExecutionTime
    public List<CareFacilityInfo> getFacilitiesByMaxTuitionFee(Integer maxFee) {
        List<CareFacility> facilities = careFacilityRepository.findByMaxTuitionFee(maxFee);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 키워드로 시설 검색
    @LogExecutionTime
    public List<CareFacilityInfo> searchFacilitiesByKeyword(String keyword) {
        List<CareFacility> facilities = careFacilityRepository.searchByKeyword(keyword);
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 복합 조건으로 시설 검색 (고급 검색)
    @LogExecutionTime
    public List<CareFacilityInfo> searchFacilitiesAdvanced(
            FacilityType facilityType,
            Boolean isPublic,
            Boolean subsidyAvailable,
            Double minRating,
            Integer minAvailableSpots,
            Integer maxTuitionFee,
            Integer childAge) {
        List<CareFacility> facilities = careFacilityRepository.searchFacilities(
                facilityType, isPublic, subsidyAvailable, minRating,
                minAvailableSpots, maxTuitionFee, childAge
        );
        return facilities.stream()
                .map(careFacilityMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 리뷰와 함께 시설 상세 조회
    @LogExecutionTime
    public CareFacilityInfo getFacilityByIdWithReviews(Long facilityId) {
        CareFacility facility = careFacilityRepository.findByIdWithReviews(facilityId)
                .orElseThrow(() -> new CareFacilityNotFoundException("돌봄 시설을 찾을 수 없습니다: " + facilityId));
        return careFacilityMapper.toResponse(facility);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getFacilityReviews(Long facilityId) {
        CareFacility facility = careFacilityRepository.findById(facilityId)
                .orElseThrow(() -> new CareFacilityNotFoundException("돌봄 시설을 찾을 수 없습니다: " + facilityId));
        return reviewRepository.findByCareFacilityOrderByCreatedAtDesc(facility).stream()
                .map(this::toReviewResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponse createReview(Long facilityId, String userEmail, ReviewRequest request) {
        CareFacility facility = careFacilityRepository.findById(facilityId)
                .orElseThrow(() -> new CareFacilityNotFoundException("돌봄 시설을 찾을 수 없습니다: " + facilityId));
        User user = userRepository.findByEmailAndDeletedAtIsNull(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Review review = Review.builder()
                .careFacility(facility)
                .user(user)
                .rating(request.getRating())
                .content(request.getContent())
                .build();
        Review savedReview = reviewRepository.save(review);

        long reviewCount = reviewRepository.countByCareFacility(facility);
        double avgRating = reviewRepository.findByCareFacilityOrderByCreatedAtDesc(facility).stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        facility.setReviewCount((int) reviewCount);
        facility.setRating(avgRating);
        careFacilityRepository.save(facility);

        return toReviewResponse(savedReview);
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, String userEmail, ReviewRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Review review = reviewRepository.findByIdAndUser(reviewId, user)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없거나 권한이 없습니다."));
        review.updateReview(request.getRating(), request.getContent());
        return toReviewResponse(reviewRepository.save(review));
    }

    @Transactional
    public void deleteReview(Long reviewId, String userEmail) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Review review = reviewRepository.findByIdAndUser(reviewId, user)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없거나 권한이 없습니다."));
        reviewRepository.delete(review);
    }

    private ReviewResponse toReviewResponse(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getId())
                .facilityId(review.getCareFacility().getId())
                .userId(review.getUser().getUserId())
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    // Entity를 DTO로 변환

    // 매핑은 CareFacilityMapper 사용
} 