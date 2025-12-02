package com.carecode.domain.careFacility.repository;

import com.carecode.domain.careFacility.dto.response.TypeStats;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.FacilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 육아 시설 리포지토리 인터페이스
 */
@Repository
public interface CareFacilityRepository extends JpaRepository<CareFacility, Long> {
    
    /**
     * 시설 코드로 시설 조회
     */
    Optional<CareFacility> findByFacilityCode(String facilityCode);
    
    /**
     * 활성화된 시설 목록 조회
     */
    List<CareFacility> findByIsActiveTrue();
    
    /**
     * 시설 유형별 조회
     */
    List<CareFacility> findByFacilityType(FacilityType facilityType);
    
    /**
     * 지역별 시설 조회
     */
    List<CareFacility> findByAddressContaining(String region);
    
    /**
     * 연령 범위에 해당하는 시설 조회
     */
    @Query("SELECT cf FROM CareFacility cf WHERE cf.isActive = true AND " +
           "((cf.ageRangeMin IS NULL OR cf.ageRangeMin <= :childAge) AND " +
           "(cf.ageRangeMax IS NULL OR cf.ageRangeMax >= :childAge))")
    List<CareFacility> findByChildAge(@Param("childAge") Integer childAge);
    
    /**
     * 최소 평점 이상의 시설 조회
     */
    @Query("SELECT cf FROM CareFacility cf WHERE cf.isActive = true AND cf.rating >= :minRating")
    List<CareFacility> findByMinRating(@Param("minRating") Double minRating);
    
    /**
     * 공립/사립 구분으로 시설 조회
     */
    List<CareFacility> findByIsPublic(Boolean isPublic);
    
    /**
     * 보조금 지원 가능한 시설 조회
     */
    List<CareFacility> findBySubsidyAvailableTrue();
    
    /**
     * 빈 자리가 있는 시설 조회
     */
    @Query("SELECT cf FROM CareFacility cf WHERE cf.isActive = true AND " +
           "cf.availableSpots > 0 AND cf.availableSpots >= :minSpots")
    List<CareFacility> findByAvailableSpots(@Param("minSpots") Integer minSpots);
    
    /**
     * 등록금 범위로 시설 조회
     */
    @Query("SELECT cf FROM CareFacility cf WHERE cf.isActive = true AND " +
           "cf.tuitionFee <= :maxFee")
    List<CareFacility> findByMaxTuitionFee(@Param("maxFee") Integer maxFee);
    
    /**
     * 키워드로 시설 검색
     */
    @Query("SELECT cf FROM CareFacility cf WHERE cf.isActive = true AND " +
           "(cf.name LIKE %:keyword% OR cf.address LIKE %:keyword%)")
    List<CareFacility> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 거리 기반 시설 검색 (위도/경도 기준)
     */
    @Query(value = "SELECT cf.*, " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(cf.latitude)) * " +
           "cos(radians(cf.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(cf.latitude)))) AS distance " +
           "FROM care_facilities cf " +
           "WHERE cf.is_active = true " +
           "HAVING distance <= :radiusKm " +
           "ORDER BY distance", nativeQuery = true)
    List<CareFacility> findByLocationAndRadius(@Param("latitude") Double latitude,
                                              @Param("longitude") Double longitude,
                                              @Param("radiusKm") Double radiusKm);
    
    /**
     * 복합 조건으로 시설 검색
     */
    @Query("SELECT cf FROM CareFacility cf WHERE cf.isActive = true " +
           "AND (:facilityType IS NULL OR cf.facilityType = :facilityType) " +
           "AND (:isPublic IS NULL OR cf.isPublic = :isPublic) " +
           "AND (:subsidyAvailable IS NULL OR cf.subsidyAvailable = :subsidyAvailable) " +
           "AND (:minRating IS NULL OR cf.rating >= :minRating) " +
           "AND (:minAvailableSpots IS NULL OR cf.availableSpots >= :minAvailableSpots) " +
           "AND (:maxTuitionFee IS NULL OR cf.tuitionFee <= :maxTuitionFee) " +
           "AND ((:childAge IS NULL) OR " +
           "((cf.ageRangeMin IS NULL OR cf.ageRangeMin <= :childAge) AND " +
           "(cf.ageRangeMax IS NULL OR cf.ageRangeMax >= :childAge))) " +
           "ORDER BY cf.rating DESC, cf.reviewCount DESC")
    List<CareFacility> searchFacilities(@Param("facilityType") FacilityType facilityType,
                                       @Param("isPublic") Boolean isPublic,
                                       @Param("subsidyAvailable") Boolean subsidyAvailable,
                                       @Param("minRating") Double minRating,
                                       @Param("minAvailableSpots") Integer minAvailableSpots,
                                       @Param("maxTuitionFee") Integer maxTuitionFee,
                                       @Param("childAge") Integer childAge);
    
    /**
     * 전체 조회수 합계 조회
     */
    @Query("SELECT COALESCE(SUM(cf.viewCount), 0) FROM CareFacility cf WHERE cf.isActive = true")
    long getTotalViewCount();
    
    /**
     * 시설 유형별 통계 조회
     */
    @Query("SELECT new com.carecode.domain.careFacility.dto.TypeStats(" +
           "cf.facilityType, COUNT(cf), COALESCE(AVG(cf.rating), 0.0), COALESCE(SUM(cf.viewCount), 0)) " +
           "FROM CareFacility cf WHERE cf.isActive = true " +
           "GROUP BY cf.facilityType")
    List<TypeStats> getTypeStats();
    
    /**
     * 연령대별 시설 조회
     */
    @Query("SELECT cf FROM CareFacility cf WHERE cf.isActive = true AND " +
           "cf.ageRangeMin <= :maxAge AND cf.ageRangeMax >= :minAge")
    List<CareFacility> findByAgeRange(@Param("minAge") int minAge, @Param("maxAge") int maxAge);
    
    /**
     * 운영 시간별 시설 조회
     */
    @Query("SELECT cf FROM CareFacility cf WHERE cf.isActive = true AND " +
           "cf.operatingHours LIKE %:operatingHours%")
    List<CareFacility> findByOperatingHours(@Param("operatingHours") String operatingHours);
    
    /**
     * 인기 시설 조회 (평점 기준)
     */
    @Query("SELECT cf FROM CareFacility cf WHERE cf.isActive = true " +
           "ORDER BY cf.rating DESC, cf.reviewCount DESC")
    List<CareFacility> findPopularFacilities(org.springframework.data.domain.Pageable pageable);
    
    /**
     * 신규 시설 조회
     */
    @Query("SELECT cf FROM CareFacility cf WHERE cf.isActive = true " +
           "ORDER BY cf.createdAt DESC")
    List<CareFacility> findNewFacilities(org.springframework.data.domain.Pageable pageable);
    
    /**
     * 반경 내 시설 조회
     */
    @Query(value = "SELECT cf.*, " +
           "(6371 * acos(cos(radians(:latitude)) * cos(radians(cf.latitude)) * " +
           "cos(radians(cf.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(cf.latitude)))) AS distance " +
           "FROM care_facilities cf " +
           "WHERE cf.is_active = true " +
           "HAVING distance <= :radius " +
           "ORDER BY distance", nativeQuery = true)
    List<CareFacility> findWithinRadius(@Param("latitude") Double latitude,
                                       @Param("longitude") Double longitude,
                                       @Param("radius") Double radius);
    
    /**
     * 검색 조건으로 시설 검색 (페이징)
     */
    @Query("SELECT cf FROM CareFacility cf WHERE cf.isActive = true " +
           "AND (:keyword IS NULL OR cf.name LIKE %:keyword% OR cf.address LIKE %:keyword%) " +
           "AND (:facilityType IS NULL OR cf.facilityType = :facilityType) " +
           "AND (:address IS NULL OR cf.address LIKE %:address%)")
    org.springframework.data.domain.Page<CareFacility> findBySearchCriteria(
            @Param("keyword") String keyword,
            @Param("facilityType") FacilityType facilityType,
            @Param("address") String address,
            org.springframework.data.domain.Pageable pageable);

    /**
     * 모든 시설 조회 (Reviews와 함께 Fetch Join으로 N+1 문제 방지)
     */
    @Query("SELECT DISTINCT cf FROM CareFacility cf LEFT JOIN FETCH cf.reviews")
    List<CareFacility> findAllWithReviews();

    /**
     * 활성화된 시설 목록 조회 (Reviews와 함께)
     */
    @Query("SELECT DISTINCT cf FROM CareFacility cf LEFT JOIN FETCH cf.reviews WHERE cf.isActive = true")
    List<CareFacility> findActiveWithReviews();

    /**
     * ID로 시설 조회 (Reviews와 함께)
     */
    @Query("SELECT cf FROM CareFacility cf LEFT JOIN FETCH cf.reviews WHERE cf.id = :id")
    Optional<CareFacility> findByIdWithReviews(@Param("id") Long id);
} 