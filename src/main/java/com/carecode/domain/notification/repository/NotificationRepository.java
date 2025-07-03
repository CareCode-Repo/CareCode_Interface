package com.carecode.domain.notification.repository;

import com.carecode.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 리포지토리 인터페이스
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * 사용자별 알림 목록 조회
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    /**
     * 읽지 않은 알림 조회
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);
    
    /**
     * 알림 타입별 조회
     */
    List<Notification> findByUserIdAndNotificationTypeOrderByCreatedAtDesc(String userId, String notificationType);
    
    /**
     * 우선순위별 알림 조회
     */
    List<Notification> findByUserIdAndPriorityOrderByCreatedAtDesc(String userId, String priority);
    
    /**
     * 전송되지 않은 알림 조회
     */
    List<Notification> findByIsSentFalseAndScheduledAtBefore(LocalDateTime now);
    
    /**
     * 예약된 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.scheduledAt >= :now ORDER BY n.scheduledAt ASC")
    List<Notification> findScheduledNotifications(@Param("userId") String userId, @Param("now") LocalDateTime now);
    
    /**
     * 기간별 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.createdAt BETWEEN :startDate AND :endDate")
    List<Notification> findByDateRange(@Param("userId") String userId, 
                                      @Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * 사용자별 알림 개수 조회
     */
    long countByUserId(String userId);
    
    /**
     * 읽지 않은 알림 개수 조회
     */
    long countByUserIdAndIsReadFalse(String userId);
    
    /**
     * 알림 타입별 개수 조회
     */
    long countByUserIdAndNotificationType(String userId, String notificationType);
} 