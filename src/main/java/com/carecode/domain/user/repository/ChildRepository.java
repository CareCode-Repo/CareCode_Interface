package com.carecode.domain.user.repository;

import com.carecode.domain.user.entity.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 자녀 리포지토리 인터페이스
 */
@Repository
public interface ChildRepository extends JpaRepository<Child, Long> {
    
    /**
     * 사용자별 자녀 목록 조회
     */
    List<Child> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 연령 범위별 자녀 조회
     */
    @Query("SELECT c FROM Child c WHERE c.user.id = :userId AND c.age >= :minAge AND c.age <= :maxAge")
    List<Child> findByUserIdAndAgeRange(@Param("userId") Long userId, 
                                       @Param("minAge") Integer minAge, 
                                       @Param("maxAge") Integer maxAge);
    
    /**
     * 성별 자녀 조회
     */
    List<Child> findByUserIdAndGender(Long userId, String gender);
    
    /**
     * 특별한 요구사항이 있는 자녀 조회
     */
    @Query("SELECT c FROM Child c WHERE c.user.id = :userId AND c.specialNeeds IS NOT NULL AND c.specialNeeds != ''")
    List<Child> findByUserIdAndHasSpecialNeeds(@Param("userId") Long userId);
    
    /**
     * 사용자별 자녀 수 조회
     */
    long countByUserId(Long userId);
    
    /**
     * 이름으로 자녀 검색
     */
    List<Child> findByUserIdAndNameContaining(Long userId, String name);
} 