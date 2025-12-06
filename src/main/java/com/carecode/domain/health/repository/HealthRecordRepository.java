package com.carecode.domain.health.repository;

import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 건강 기록 리포지토리 인터페이스
 */
@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

    // 아동별 건강 기록 조회 (최신순)
    Page<HealthRecord> findByChildIdOrderByRecordDateDesc(Long childId, Pageable pageable);

    // 아동별 건강 기록 조회 (최신순) - JOIN FETCH로 N+1 문제 해결
    @Query("SELECT r FROM HealthRecord r " +
           "JOIN FETCH r.child c " +
           "JOIN FETCH r.user u " +
           "WHERE r.child.id = :childId " +
           "ORDER BY r.recordDate DESC")
    List<HealthRecord> findByChildIdWithChildAndUser(@Param("childId") Long childId);

    // 아동별 건강 기록 조회 (최신순)
    List<HealthRecord> findByChildOrderByRecordDateDesc(Child child);

    // 사용자별 건강 기록 조회 (최신순) - JOIN FETCH로 N+1 문제 해결
    @Query("SELECT r FROM HealthRecord r " +
           "JOIN FETCH r.child c " +
           "JOIN FETCH r.user u " +
           "WHERE r.user.id = :userId " +
           "ORDER BY r.recordDate DESC")
    List<HealthRecord> findByUserIdWithChildAndUser(@Param("userId") Long userId);

    // 사용자별 건강 기록 조회 (최신순)
    List<HealthRecord> findByUserOrderByRecordDateDesc(User user);

    // 기간별 건강 기록 조회 (최신순) - JOIN FETCH로 N+1 문제 해결
    @Query("SELECT r FROM HealthRecord r " +
           "JOIN FETCH r.child c " +
           "JOIN FETCH r.user u " +
           "WHERE r.child.id = :childId " +
           "AND r.recordDate BETWEEN :startDate AND :endDate " +
           "ORDER BY r.recordDate DESC")
    List<HealthRecord> findByChildIdAndRecordDateBetweenWithChildAndUser(
            @Param("childId") Long childId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 기간별 건강 기록 조회 (최신순)
    List<HealthRecord> findByChildIdAndRecordDateBetweenOrderByRecordDateDesc(Long childId, LocalDate startDate, LocalDate endDate);

    // 기간별 건강 기록 조회 (오래된순) - JOIN FETCH로 N+1 문제 해결
    @Query("SELECT r FROM HealthRecord r " +
           "JOIN FETCH r.child c " +
           "JOIN FETCH r.user u " +
           "WHERE r.child.id = :childId " +
           "AND r.recordDate BETWEEN :startDate AND :endDate " +
           "ORDER BY r.recordDate ASC")
    List<HealthRecord> findByChildIdAndRecordDateBetweenOrderByRecordDateAscWithChildAndUser(
            @Param("childId") Long childId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 기간별 건강 기록 조회 (오래된순)
    List<HealthRecord> findByChildIdAndRecordDateBetweenOrderByRecordDateAsc(Long childId, LocalDate startDate, LocalDate endDate);

    // 아동별 특정 타입의 건강 기록 조회 - JOIN FETCH로 N+1 문제 해결
    @Query("SELECT r FROM HealthRecord r " +
           "JOIN FETCH r.child c " +
           "JOIN FETCH r.user u " +
           "WHERE r.child.id = :childId " +
           "AND r.recordType = :recordType " +
           "ORDER BY r.recordDate DESC")
    List<HealthRecord> findByChildIdAndRecordTypeWithChildAndUser(
            @Param("childId") Long childId,
            @Param("recordType") HealthRecord.RecordType recordType);

    // 아동별 특정 타입의 건강 기록 조회
    List<HealthRecord> findByChildIdAndRecordType(Long childId, HealthRecord.RecordType recordType);
} 