package com.carecode.domain.health.repository;

import com.carecode.domain.health.entity.HealthRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 건강 기록 리포지토리 인터페이스
 */
@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {
    
    /**
     * 사용자별 건강 기록 조회
     */
    Page<HealthRecord> findByUserIdOrderByRecordDateDesc(String userId, Pageable pageable);
    
    /**
     * 자녀별 건강 기록 조회
     */
    Page<HealthRecord> findByChildIdOrderByRecordDateDesc(String childId, Pageable pageable);
    
    /**
     * 기록 타입별 조회
     */
    List<HealthRecord> findByUserIdAndRecordTypeOrderByRecordDateDesc(String userId, String recordType);
    
    /**
     * 완료된 기록 조회
     */
    List<HealthRecord> findByUserIdAndIsCompletedTrue(String userId);
    
    /**
     * 미완료된 기록 조회
     */
    List<HealthRecord> findByUserIdAndIsCompletedFalse(String userId);
    
    /**
     * 다가오는 일정 조회
     */
    @Query("SELECT h FROM HealthRecord h WHERE h.userId = :userId AND h.nextDate >= :now ORDER BY h.nextDate ASC")
    List<HealthRecord> findUpcomingRecords(@Param("userId") String userId, @Param("now") LocalDateTime now);
    
    /**
     * 기간별 기록 조회
     */
    @Query("SELECT h FROM HealthRecord h WHERE h.userId = :userId AND h.recordDate BETWEEN :startDate AND :endDate")
    List<HealthRecord> findByDateRange(@Param("userId") String userId, 
                                      @Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * 사용자별 기록 개수 조회
     */
    long countByUserId(String userId);
    
    /**
     * 자녀별 기록 개수 조회
     */
    long countByChildId(String childId);
    
    /**
     * 완료된 기록 개수 조회
     */
    long countByUserIdAndIsCompletedTrue(String userId);
} 