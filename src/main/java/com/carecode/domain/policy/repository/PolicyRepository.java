package com.carecode.domain.policy.repository;

import com.carecode.domain.policy.dto.response.PolicyCategoryStatsResponse;
import com.carecode.domain.policy.entity.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // 정책 코드로 정책 조회
    Optional<Policy> findByPolicyCode(String policyCode);

    // 활성화된 정책 목록 조회
    List<Policy> findByIsActiveTrue();

    // 정책 유형별 조회
    List<Policy> findByPolicyType(String policyType);

    List<Policy> findTop1ByOrderByCreatedAtDesc();

    // 지역별 정책 조회
    List<Policy> findByTargetRegion(String targetRegion);

    // 연령 범위에 해당하는 정책 조회
    @Query("SELECT p FROM Policy p WHERE p.isActive = true AND " +
           "((p.targetAgeMin IS NULL OR p.targetAgeMin <= :childAge) AND " +
           "(p.targetAgeMax IS NULL OR p.targetAgeMax >= :childAge))")
    List<Policy> findByChildAge(@Param("childAge") Integer childAge);

    // 신청 기간이 유효한 정책 조회
    @Query("SELECT p FROM Policy p WHERE p.isActive = true AND " +
           "p.applicationStartDate <= :today AND p.applicationEndDate >= :today")
    List<Policy> findActivePoliciesByDate(@Param("today") LocalDate today);

    // 키워드로 정책 검색
    @Query("SELECT p FROM Policy p WHERE p.isActive = true AND " +
           "(p.title LIKE %:keyword% OR p.description LIKE %:keyword%)")
    List<Policy> searchByKeyword(@Param("keyword") String keyword);

    // 복합 조건으로 정책 검색
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
    

    // 연령대별 정책 조회

    @Query("SELECT p FROM Policy p WHERE p.isActive = true AND " +
           "p.targetAgeMin <= :maxAge AND p.targetAgeMax >= :minAge")
    List<Policy> findByAgeRange(@Param("minAge") int minAge, @Param("maxAge") int maxAge);
    

    // 인기 정책 조회 (우선순위 기준)

    @Query("SELECT p FROM Policy p WHERE p.isActive = true " +
           "ORDER BY p.priority DESC, p.createdAt DESC")
    List<Policy> findPopularPolicies(Pageable pageable);
    

    // 최신 정책 조회

    @Query("SELECT p FROM Policy p WHERE p.isActive = true " +
           "ORDER BY p.createdAt DESC")
    List<Policy> findLatestPolicies(Pageable pageable);
    

    // 검색 조건으로 정책 검색 (페이징)

    @Query("SELECT p FROM Policy p WHERE p.isActive = true " +
           "AND (:keyword IS NULL OR p.title LIKE %:keyword% OR p.description LIKE %:keyword%) " +
           "AND (:category IS NULL OR p.policyType = :category) " +
           "AND (:location IS NULL OR p.targetRegion LIKE %:location%) " +
           "AND (:minAge IS NULL OR p.targetAgeMin IS NULL OR p.targetAgeMin <= :minAge) " +
           "AND (:maxAge IS NULL OR p.targetAgeMax IS NULL OR p.targetAgeMax >= :maxAge)")
    Page<Policy> findBySearchCriteria(@Param("keyword") String keyword, @Param("category") String category,
                                      @Param("location") String location, @Param("minAge") Integer minAge,
                                      @Param("maxAge") Integer maxAge, Pageable pageable);
    

    @Query("SELECT COALESCE(SUM(p.viewCount), 0) FROM Policy p WHERE p.isActive = true")
    long getTotalViewCount();
    

    // 카테고리별 통계 조회

    // HQL 의 생성자 표현식은 완전한 패키지 경로를 요구한다.
    // 실제 클래스는 dto.response 패키지에 있으므로 경로가 틀리면 기동 시점에 실패한다.
    @Query("SELECT new com.carecode.domain.policy.dto.response.PolicyCategoryStatsResponse(" +
           "p.policyType, COUNT(p), 0.0, 0) " +
           "FROM Policy p WHERE p.isActive = true " +
           "GROUP BY p.policyType")
    List<PolicyCategoryStatsResponse> getCategoryStats();

    /**
     * 조회수를 DB 에서 원자적으로 증가시킨다.
     * 엔티티를 읽어 +1 후 save 하면 동시 요청 시 증가분이 유실된다(lost update).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Policy p SET p.viewCount = COALESCE(p.viewCount, 0) + 1 WHERE p.id = :policyId")
    int incrementViewCount(@Param("policyId") Long policyId);

    /**
     * 중복 없는 정책 유형 목록. 전체 행을 메모리로 올려 distinct 하지 않는다.
     */
    @Query("SELECT DISTINCT p.policyType FROM Policy p " +
           "WHERE p.policyType IS NOT NULL AND p.policyType <> '' " +
           "ORDER BY p.policyType")
    List<String> findDistinctPolicyTypes();
} 