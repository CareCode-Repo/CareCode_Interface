package com.carecode.domain.notification.app;

import com.carecode.domain.notification.dto.NotificationRequest;
import com.carecode.domain.notification.dto.NotificationResponse;
import com.carecode.domain.notification.service.NotificationPreferenceService;
import com.carecode.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationFacade {

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;

    @Transactional(readOnly = true)
    public List<NotificationResponse.Notification> getNotificationsByUserId(String userId) {
        return notificationService.getNotificationsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public NotificationResponse.Notification getNotificationById(Long notificationId) {
        return notificationService.getNotificationById(notificationId);
    }

    @Transactional
    public NotificationResponse.Notification createNotification(NotificationRequest.Create request) {
        return notificationService.createNotification(request);
    }

    @Transactional
    public NotificationResponse.Notification updateNotification(Long id, NotificationRequest.Create request) {
        return notificationService.updateNotification(id, request);
    }

    @Transactional
    public void deleteNotification(Long id) { notificationService.deleteNotification(id); }

    @Transactional
    public void markAsRead(Long id) { notificationService.markAsRead(id); }

    @Transactional
    public void markAllAsRead(String userId) { notificationService.markAllAsRead(userId); }

    @Transactional(readOnly = true)
    public List<NotificationResponse.Notification> getUnreadNotifications(String userId) {
        return notificationService.getUnreadNotifications(userId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationSettings(String userId) { return notificationService.getNotificationSettings(userId); }

    @Transactional
    public Map<String, Object> updateNotificationSettings(String userId, Map<String, Object> settings) { return notificationService.updateNotificationSettings(userId, settings); }

    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationStatistics(String userId) { return notificationService.getNotificationStatistics(userId); }

    @Transactional(readOnly = true)
    public List<NotificationResponse.Settings> getUserPreferences(String userId) { return preferenceService.getUserPreferences(userId); }

    @Transactional(readOnly = true)
    public NotificationResponse.Settings getPreferenceByType(String userId, String notificationType) {
        return preferenceService.getPreferenceByType(userId, com.carecode.domain.notification.entity.Notification.NotificationType.valueOf(notificationType));
    }

    @Transactional
    public NotificationResponse.Settings savePreference(String userId, NotificationResponse.Settings dto) { return preferenceService.savePreference(userId, dto); }

    @Transactional
    public NotificationResponse.Settings updateChannelPreference(String userId, String notificationType, String channel, boolean enabled) {
        return preferenceService.updateChannelPreference(userId, notificationType, channel, enabled);
    }

    @Transactional
    public void disableAllNotifications(String userId) { preferenceService.disableAllNotifications(userId); }

    @Transactional
    public void resetToDefault(String userId) { preferenceService.resetToDefault(userId); }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(NotificationRequest.MarkAsRead request) {
        notificationService.markAsRead(request);
    }

    /**
     * 푸시 알림 토큰 등록
     */
    @Transactional
    public void registerPushToken(String userId, NotificationRequest.RegisterPushToken request) {
        preferenceService.registerPushToken(userId, request);
    }

    /**
     * 알림 설정 수정
     */
    @Transactional
    public void updateSettings(String userId, NotificationRequest.UpdateSettings request) {
        preferenceService.updateSettings(userId, request);
    }

    /**
     * 테스트 알림 발송
     */
    @Transactional
    public void sendTestNotification(String userId, NotificationRequest.SendTest request) {
        notificationService.sendTestNotification(userId, request);
    }

    /**
     * 알림 통계 조회
     */
    @Transactional(readOnly = true)
    public NotificationResponse.Stats getNotificationStats(String userId) {
        return notificationService.getNotificationStats(userId);
    }

    /**
     * 알림 템플릿 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse.Template> getNotificationTemplates(String type) {
        return notificationService.getNotificationTemplates(type);
    }

    /**
     * 알림 전송 상태 조회
     */
    @Transactional(readOnly = true)
    public NotificationResponse.DeliveryStatus getDeliveryStatus(Long notificationId) {
        return notificationService.getDeliveryStatus(notificationId);
    }
}


