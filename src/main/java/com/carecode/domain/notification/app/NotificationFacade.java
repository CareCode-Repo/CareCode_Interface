package com.carecode.domain.notification.app;

import com.carecode.domain.notification.dto.request.NotificationRequest;
import com.carecode.domain.notification.dto.request.NotificationCreateRequest;
import com.carecode.domain.notification.dto.request.NotificationMarkAsReadRequest;
import com.carecode.domain.notification.dto.request.NotificationRegisterPushTokenRequest;
import com.carecode.domain.notification.dto.request.NotificationUpdateSettingsRequest;
import com.carecode.domain.notification.dto.request.NotificationSendTestRequest;
import com.carecode.domain.notification.dto.response.NotificationResponse;
import com.carecode.domain.notification.dto.response.NotificationInfoResponse;
import com.carecode.domain.notification.dto.response.NotificationSettingsResponse;
import com.carecode.domain.notification.dto.response.NotificationStatsResponse;
import com.carecode.domain.notification.dto.response.NotificationTemplateResponse;
import com.carecode.domain.notification.dto.response.NotificationDeliveryStatusResponse;
import com.carecode.domain.notification.service.NotificationPreferenceService;
import com.carecode.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import com.carecode.domain.user.repository.UserRepository;
import com.carecode.domain.user.entity.User;

@Service
@RequiredArgsConstructor
public class NotificationFacade {

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<NotificationInfoResponse> getNotificationsByUserId(String userId) {
        return notificationService.getNotificationsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public NotificationInfoResponse getNotificationById(Long notificationId) {
        return notificationService.getNotificationById(notificationId);
    }

    @Transactional
    public NotificationInfoResponse createNotification(NotificationCreateRequest request) {
        return notificationService.createNotification(request);
    }

    @Transactional
    public NotificationInfoResponse updateNotification(Long id, NotificationCreateRequest request) {
        return notificationService.updateNotification(id, request);
    }

    @Transactional
    public void deleteNotification(Long id) { notificationService.deleteNotification(id); }

    @Transactional
    public void markAsRead(Long id) { notificationService.markAsRead(id); }

    @Transactional
    public void markAllAsRead(String userId) { notificationService.markAllAsRead(userId); }

    @Transactional(readOnly = true)
    public List<NotificationInfoResponse> getUnreadNotifications(String userId) {
        return notificationService.getUnreadNotifications(userId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationSettings(String userId) { return notificationService.getNotificationSettings(userId); }

    @Transactional
    public Map<String, Object> updateNotificationSettings(String userId, Map<String, Object> settings) { return notificationService.updateNotificationSettings(userId, settings); }

    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationStatistics(String userId) { return notificationService.getNotificationStatistics(userId); }

    @Transactional(readOnly = true)
    public List<NotificationSettingsResponse> getUserPreferences(String userId) { return preferenceService.getUserPreferences(userId); }

    @Transactional(readOnly = true)
    public NotificationSettingsResponse getPreferenceByType(String userId, String notificationType) {
        return preferenceService.getPreferenceByType(userId, com.carecode.domain.notification.entity.Notification.NotificationType.valueOf(notificationType));
    }

    @Transactional
    public NotificationSettingsResponse savePreference(String userId, NotificationSettingsResponse dto) { return preferenceService.savePreference(userId, dto); }

    @Transactional
    public NotificationSettingsResponse updateChannelPreference(String userId, String notificationType, String channel, boolean enabled) {
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
    public void markAsRead(NotificationMarkAsReadRequest request) {
        notificationService.markAsRead(request);
    }

    /**
     * 푸시 알림 토큰 등록
     */
    @Transactional
    public void registerPushToken(String userId, NotificationRegisterPushTokenRequest request) {
        preferenceService.registerPushToken(userId, request);
    }

    /**
     * 알림 설정 수정
     */
    @Transactional
    public void updateSettings(String userId, NotificationUpdateSettingsRequest request) {
        preferenceService.updateSettings(userId, request);
    }

    @Transactional(readOnly = true)
    public List<NotificationInfoResponse> getNotificationsByType(String userId, com.carecode.domain.notification.entity.Notification.NotificationType notificationType) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        return notificationService.getNotificationsByType(user.getId(), notificationType);
    }

    @Transactional(readOnly = true)
    public List<NotificationInfoResponse> getNotificationsByDateRange(String userId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        return notificationService.getNotificationsByDateRange(user.getId(), startDate, endDate);
    }

    @Transactional(readOnly = true)
    public long getTotalNotificationCount(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        return notificationService.getTotalNotificationCount(user.getId());
    }

    @Transactional(readOnly = true)
    public long getNotificationCountByReadStatus(String userId, boolean isRead) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        return notificationService.getNotificationCountByReadStatus(user.getId(), isRead);
    }

    /**
     * 테스트 알림 발송
     */
    @Transactional
    public void sendTestNotification(String userId, NotificationSendTestRequest request) {
        notificationService.sendTestNotification(userId, request);
    }

    /**
     * 알림 통계 조회
     */
    @Transactional(readOnly = true)
    public NotificationStatsResponse getNotificationStats(String userId) {
        return notificationService.getNotificationStats(userId);
    }

    /**
     * 알림 템플릿 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> getNotificationTemplates(String type) {
        return notificationService.getNotificationTemplates(type);
    }

    /**
     * 알림 전송 상태 조회
     */
    @Transactional(readOnly = true)
    public NotificationDeliveryStatusResponse getDeliveryStatus(Long notificationId) {
        return notificationService.getDeliveryStatus(notificationId);
    }
}


