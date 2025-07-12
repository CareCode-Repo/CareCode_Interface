package com.carecode.domain.user.repository;

import com.carecode.domain.user.entity.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 알림 설정 리포지토리 인터페이스
 */
@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {
    
    /**
     * 사용자별 알림 설정 조회
     */
    Optional<NotificationSettings> findByUserId(Long userId);
    
    /**
     * 특정 알림 타입이 활성화된 사용자들의 설정 조회
     */
    List<NotificationSettings> findByPolicyNotificationTrue();
    
    List<NotificationSettings> findByFacilityNotificationTrue();
    
    List<NotificationSettings> findByCommunityNotificationTrue();
    
    List<NotificationSettings> findByChatbotNotificationTrue();
    
    List<NotificationSettings> findByEmailNotificationTrue();
    
    List<NotificationSettings> findByPushNotificationTrue();
    
    List<NotificationSettings> findBySmsNotificationTrue();
    
    /**
     * 조용한 시간이 설정된 사용자들의 설정 조회
     */
    List<NotificationSettings> findByQuietHoursEnabledTrue();
} 