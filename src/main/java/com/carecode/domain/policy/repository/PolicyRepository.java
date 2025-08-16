package com.carecode.domain.policy.repository;

import com.carecode.domain.policy.dto.PolicySearchResponseDto;
import com.carecode.domain.policy.dto.CategoryStats;
import com.carecode.domain.policy.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 정책 리포지토리 인터페이스
 */
@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    
    /**
     * 정책 코드로 정책 조회
     */
    Optional<Policy> findByPolicyCode(String policyCode);
    
    /**
     * 활성화된 정책 목록 조회
     */
    List<Policy> findByIsActiveTrue();
    
    /**
     * 정책 유형별 조회
     */
    List<Policy> findByPolicyType(String policyType);
    
    /**
     * 지역별 정책 조회
     */
    List<Policy> findByTargetRegion(String targetRegion);
    
    /**
     * 연령 범위에 해당하는 정책 조회
     */
    @Query("SELECT p FROM Policy p WHERE p.isActive = true AND " +
           "((p.targetAgeMin IS NULL OR p.targetAgeMin <= :childAge) AND " +
           "(p.targetAgeMax IS NULL OR p.targetAgeMax >= :childAge))")
    List<Policy> findByChildAge(@Param("childAge") Integer childAge);
    
    /**
     * 신청 기간이 유효한 정책 조회
     */
    @Query("SELECT p FROM Policy p WHERE p.isActive = true AND " +
           "p.applicationStartDate <= :today AND p.applicationEndDate >= :today")
    List<Policy> findActivePoliciesByDate(@Param("today") LocalDate today);
    
    /**
     * 키워드로 정책 검색
     */
    @Query("SELECT p FROM Policy p WHERE p.isActive = true AND " +
           "(p.title LIKE %:keyword% OR p.description LIKE %:keyword%)")
    List<Policy> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 복합 조건으로 정책 검색
     */
    @Query("SELECT p FROM Policy p WHERE p.isActive = true " +
           "AND (:policyType IS NULL OR p.policyType = :policyType) " +
           "AND (:targetRegion IS NULL OR p.targetRegion = :targetRegion) " +
           "AND (:benefitType IS NULL OR p.benefitType = :benefitType) " +
           "AND ((:childAge IS NULL) OR " +
           "((p.targetAgeMin IS NULL OR p.targetAgeMin <= :childAge) AND " +
           "(p.targetAgeMax IS NULL OR p.targetAgeMax >= :childAge))) " +
           "ORDER BY p.priority DESC, p.createdAt DESC")
    List<Policy> searchPolicies(@Param("policyType") String policyType,
                               @Param("targetRegion") String targetRegion,
                               @Param("benefitType") String benefitType,
                               @Param("childAge") Integer childAge);
    
    /**
     * 연령대별 정책 조회
     */
    @Query("SELECT p FROM Policy p WHERE p.isActive = true AND " +
           "p.targetAgeMin <= :maxAge AND p.targetAgeMax >= :minAge")
    List<Policy> findByAgeRange(@Param("minAge") int minAge, @Param("maxAge") int maxAge);
    
    /**
     * 인기 정책 조회 (우선순위 기준)
     */
    @Query("SELECT p FROM Policy p WHERE p.isActive = true " +
           "ORDER BY p.priority DESC, p.createdAt DESC")
    List<Policy> findPopularPolicies(org.springframework.data.domain.Pageable pageable);
    
    /**
     * 최신 정책 조회
     */
    @Query("SELECT p FROM Policy p WHERE p.isActive = true " +
           "ORDER BY p.createdAt DESC")
    List<Policy> findLatestPolicies(org.springframework.data.domain.Pageable pageable);
    
    /**
     * 검색 조건으로 정책 검색 (페이징)
     */
    @Query("SELECT p FROM Policy p WHERE p.isActive = true " +
           "AND (:keyword IS NULL OR p.title LIKE %:keyword% OR p.description LIKE %:keyword%) " +
           "AND (:category IS NULL OR p.policyType = :category) " +
           "AND (:location IS NULL OR p.targetRegion LIKE %:location%) " +
           "AND (:minAge IS NULL OR p.targetAgeMin IS NULL OR p.targetAgeMin <= :minAge) " +
           "AND (:maxAge IS NULL OR p.targetAgeMax IS NULL OR p.targetAgeMax >= :maxAge)")
    org.springframework.data.domain.Page<Policy> findBySearchCriteria(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("location") String location,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            org.springframework.data.domain.Pageable pageable);
    
    /**
     * 전체 조회수 합계 조회 (현재는 0으로 반환, 추후 viewCount 필드 추가 시 수정)
     */
    @Query("SELECT COUNT(p) FROM Policy p WHERE p.isActive = true")
    long getTotalViewCount();
    
    /**
     * 카테고리별 통계 조회
     */
    @Query("SELECT new com.carecode.domain.policy.dto.CategoryStats(" +
           "p.policyType, COUNT(p), 0.0, 0) " +
           "FROM Policy p WHERE p.isActive = true " +
           "GROUP BY p.policyType")
    List<CategoryStats> getCategoryStats();
} 