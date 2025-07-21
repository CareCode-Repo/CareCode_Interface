package com.carecode.domain.health.repository;

import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 건강 기록 리포지토리 인터페이스
 */
@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {
    
    /**
     * 아동별 건강 기록 조회 (최신순)
     */
    Page<HealthRecord> findByChildIdOrderByRecordDateDesc(Long childId, Pageable pageable);
    
    /**
     * 아동별 건강 기록 조회 (최신순)
     */
    List<HealthRecord> findByChildOrderByRecordDateDesc(Child child);
    
    /**
     * 사용자별 건강 기록 조회 (최신순)
     */
    List<HealthRecord> findByUserOrderByRecordDateDesc(User user);
    
    /**
     * 기간별 건강 기록 조회 (최신순)
     */
    List<HealthRecord> findByChildIdAndRecordDateBetweenOrderByRecordDateDesc(Long childId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 기간별 건강 기록 조회 (오래된순)
     */
    List<HealthRecord> findByChildIdAndRecordDateBetweenOrderByRecordDateAsc(Long childId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 아동별 건강 기록 개수 조회
     */
    long countByChildId(Long childId);
    
    /**
     * 아동별 건강 기록 개수 조회
     */
    long countByChild(Child child);
    
    /**
     * 사용자별 건강 기록 개수 조회
     */
    long countByUser(User user);
} 