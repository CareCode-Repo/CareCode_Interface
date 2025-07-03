package com.carecode.notification.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 알림 API 컨트롤러
 * 푸시 알림, 이메일, SMS 등 다양한 알림 서비스
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    /**
     * 알림 목록 조회
     */
    @GetMapping
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        log.info("알림 목록 조회: 페이지={}, 타입={}, 읽지않음만={}", page, type, unreadOnly);
        
        // 알림 목록 조회 로직 구현
        List<NotificationResponse> notifications = List.of(); // 임시 반환
        return ResponseEntity.ok(notifications);
    }

    /**
     * 알림 상세 조회
     */
    @GetMapping("/{notificationId}")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<NotificationDetailResponse> getNotification(@PathVariable Long notificationId) {
        log.info("알림 상세 조회: 알림ID={}", notificationId);
        
        // 알림 상세 조회 로직 구현
        NotificationDetailResponse notification = new NotificationDetailResponse();
        return ResponseEntity.ok(notification);
    }

    /**
     * 알림 읽음 처리
     */
    @PutMapping("/{notificationId}/read")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long notificationId) {
        log.info("알림 읽음 처리: 알림ID={}", notificationId);
        
        // 알림 읽음 처리 로직 구현
        return ResponseEntity.ok(Map.of("message", "알림이 읽음 처리되었습니다."));
    }

    /**
     * 모든 알림 읽음 처리
     */
    @PutMapping("/read-all")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        log.info("모든 알림 읽음 처리");
        
        // 모든 알림 읽음 처리 로직 구현
        return ResponseEntity.ok(Map.of("message", "모든 알림이 읽음 처리되었습니다."));
    }

    /**
     * 알림 삭제
     */
    @DeleteMapping("/{notificationId}")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long notificationId) {
        log.info("알림 삭제: 알림ID={}", notificationId);
        
        // 알림 삭제 로직 구현
        return ResponseEntity.ok(Map.of("message", "알림이 삭제되었습니다."));
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @GetMapping("/unread-count")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        log.info("읽지 않은 알림 개수 조회");
        
        // 읽지 않은 알림 개수 조회 로직 구현
        UnreadCountResponse response = new UnreadCountResponse();
        response.setUnreadCount(5);
        return ResponseEntity.ok(response);
    }

    /**
     * 푸시 알림 토큰 등록
     */
    @PostMapping("/push-token")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<Map<String, String>> registerPushToken(@RequestBody RegisterPushTokenRequest request) {
        log.info("푸시 알림 토큰 등록: 디바이스={}", request.getDeviceType());
        
        // 푸시 알림 토큰 등록 로직 구현
        return ResponseEntity.ok(Map.of("message", "푸시 알림 토큰이 등록되었습니다."));
    }

    /**
     * 푸시 알림 토큰 삭제
     */
    @DeleteMapping("/push-token")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<Map<String, String>> unregisterPushToken() {
        log.info("푸시 알림 토큰 삭제");
        
        // 푸시 알림 토큰 삭제 로직 구현
        return ResponseEntity.ok(Map.of("message", "푸시 알림 토큰이 삭제되었습니다."));
    }

    /**
     * 알림 설정 조회
     */
    @GetMapping("/settings")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<NotificationSettingsResponse> getNotificationSettings() {
        log.info("알림 설정 조회");
        
        // 알림 설정 조회 로직 구현
        NotificationSettingsResponse settings = new NotificationSettingsResponse();
        return ResponseEntity.ok(settings);
    }

    /**
     * 알림 설정 수정
     */
    @PutMapping("/settings")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<NotificationSettingsResponse> updateNotificationSettings(
            @RequestBody UpdateNotificationSettingsRequest request) {
        log.info("알림 설정 수정");
        
        // 알림 설정 수정 로직 구현
        NotificationSettingsResponse settings = new NotificationSettingsResponse();
        return ResponseEntity.ok(settings);
    }

    /**
     * 테스트 알림 발송
     */
    @PostMapping("/test")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<Map<String, String>> sendTestNotification(@RequestBody SendTestNotificationRequest request) {
        log.info("테스트 알림 발송: 타입={}", request.getType());
        
        // 테스트 알림 발송 로직 구현
        return ResponseEntity.ok(Map.of("message", "테스트 알림이 발송되었습니다."));
    }

    /**
     * 알림 통계 조회
     */
    @GetMapping("/stats")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<NotificationStatsResponse> getNotificationStats(
            @RequestParam(required = false) String period) {
        log.info("알림 통계 조회: 기간={}", period);
        
        // 알림 통계 조회 로직 구현
        NotificationStatsResponse stats = new NotificationStatsResponse();
        return ResponseEntity.ok(stats);
    }

    /**
     * 알림 템플릿 목록 조회
     */
    @GetMapping("/templates")
    @LogExecutionTime
    public ResponseEntity<List<NotificationTemplateResponse>> getNotificationTemplates() {
        log.info("알림 템플릿 목록 조회");
        
        // 알림 템플릿 목록 조회 로직 구현
        List<NotificationTemplateResponse> templates = List.of(); // 임시 반환
        return ResponseEntity.ok(templates);
    }

    // DTO 클래스들
    public static class RegisterPushTokenRequest {
        private String token;
        private String deviceType; // "ANDROID", "IOS", "WEB"
        private String deviceId;
        
        // getters and setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    }

    public static class UpdateNotificationSettingsRequest {
        private boolean policyNotification;
        private boolean facilityNotification;
        private boolean communityNotification;
        private boolean chatbotNotification;
        private boolean emailNotification;
        private boolean pushNotification;
        private boolean smsNotification;
        private String quietHoursStart;
        private String quietHoursEnd;
        
        // getters and setters
        public boolean isPolicyNotification() { return policyNotification; }
        public void setPolicyNotification(boolean policyNotification) { this.policyNotification = policyNotification; }
        public boolean isFacilityNotification() { return facilityNotification; }
        public void setFacilityNotification(boolean facilityNotification) { this.facilityNotification = facilityNotification; }
        public boolean isCommunityNotification() { return communityNotification; }
        public void setCommunityNotification(boolean communityNotification) { this.communityNotification = communityNotification; }
        public boolean isChatbotNotification() { return chatbotNotification; }
        public void setChatbotNotification(boolean chatbotNotification) { this.chatbotNotification = chatbotNotification; }
        public boolean isEmailNotification() { return emailNotification; }
        public void setEmailNotification(boolean emailNotification) { this.emailNotification = emailNotification; }
        public boolean isPushNotification() { return pushNotification; }
        public void setPushNotification(boolean pushNotification) { this.pushNotification = pushNotification; }
        public boolean isSmsNotification() { return smsNotification; }
        public void setSmsNotification(boolean smsNotification) { this.smsNotification = smsNotification; }
        public String getQuietHoursStart() { return quietHoursStart; }
        public void setQuietHoursStart(String quietHoursStart) { this.quietHoursStart = quietHoursStart; }
        public String getQuietHoursEnd() { return quietHoursEnd; }
        public void setQuietHoursEnd(String quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }
    }

    public static class SendTestNotificationRequest {
        private String type; // "PUSH", "EMAIL", "SMS"
        private String title;
        private String message;
        
        // getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class NotificationResponse {
        private Long notificationId;
        private String type;
        private String title;
        private String message;
        private String category;
        private boolean isRead;
        private String createdAt;
        private String actionUrl;
        private Map<String, Object> metadata;
        
        // getters and setters
        public Long getNotificationId() { return notificationId; }
        public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public boolean isRead() { return isRead; }
        public void setRead(boolean read) { isRead = read; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getActionUrl() { return actionUrl; }
        public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    public static class NotificationDetailResponse extends NotificationResponse {
        private String readAt;
        private String deliveredAt;
        private String failureReason;
        
        // getters and setters
        public String getReadAt() { return readAt; }
        public void setReadAt(String readAt) { this.readAt = readAt; }
        public String getDeliveredAt() { return deliveredAt; }
        public void setDeliveredAt(String deliveredAt) { this.deliveredAt = deliveredAt; }
        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    }

    public static class UnreadCountResponse {
        private int unreadCount;
        private Map<String, Integer> unreadCountByCategory;
        
        // getters and setters
        public int getUnreadCount() { return unreadCount; }
        public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
        public Map<String, Integer> getUnreadCountByCategory() { return unreadCountByCategory; }
        public void setUnreadCountByCategory(Map<String, Integer> unreadCountByCategory) { this.unreadCountByCategory = unreadCountByCategory; }
    }

    public static class NotificationSettingsResponse {
        private boolean policyNotification;
        private boolean facilityNotification;
        private boolean communityNotification;
        private boolean chatbotNotification;
        private boolean emailNotification;
        private boolean pushNotification;
        private boolean smsNotification;
        private String quietHoursStart;
        private String quietHoursEnd;
        private boolean quietHoursEnabled;
        
        // getters and setters
        public boolean isPolicyNotification() { return policyNotification; }
        public void setPolicyNotification(boolean policyNotification) { this.policyNotification = policyNotification; }
        public boolean isFacilityNotification() { return facilityNotification; }
        public void setFacilityNotification(boolean facilityNotification) { this.facilityNotification = facilityNotification; }
        public boolean isCommunityNotification() { return communityNotification; }
        public void setCommunityNotification(boolean communityNotification) { this.communityNotification = communityNotification; }
        public boolean isChatbotNotification() { return chatbotNotification; }
        public void setChatbotNotification(boolean chatbotNotification) { this.chatbotNotification = chatbotNotification; }
        public boolean isEmailNotification() { return emailNotification; }
        public void setEmailNotification(boolean emailNotification) { this.emailNotification = emailNotification; }
        public boolean isPushNotification() { return pushNotification; }
        public void setPushNotification(boolean pushNotification) { this.pushNotification = pushNotification; }
        public boolean isSmsNotification() { return smsNotification; }
        public void setSmsNotification(boolean smsNotification) { this.smsNotification = smsNotification; }
        public String getQuietHoursStart() { return quietHoursStart; }
        public void setQuietHoursStart(String quietHoursStart) { this.quietHoursStart = quietHoursStart; }
        public String getQuietHoursEnd() { return quietHoursEnd; }
        public void setQuietHoursEnd(String quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }
        public boolean isQuietHoursEnabled() { return quietHoursEnabled; }
        public void setQuietHoursEnabled(boolean quietHoursEnabled) { this.quietHoursEnabled = quietHoursEnabled; }
    }

    public static class NotificationStatsResponse {
        private int totalNotifications;
        private int readNotifications;
        private int unreadNotifications;
        private Map<String, Integer> notificationsByType;
        private Map<String, Integer> notificationsByCategory;
        private Map<String, Integer> dailyNotificationCount;
        
        // getters and setters
        public int getTotalNotifications() { return totalNotifications; }
        public void setTotalNotifications(int totalNotifications) { this.totalNotifications = totalNotifications; }
        public int getReadNotifications() { return readNotifications; }
        public void setReadNotifications(int readNotifications) { this.readNotifications = readNotifications; }
        public int getUnreadNotifications() { return unreadNotifications; }
        public void setUnreadNotifications(int unreadNotifications) { this.unreadNotifications = unreadNotifications; }
        public Map<String, Integer> getNotificationsByType() { return notificationsByType; }
        public void setNotificationsByType(Map<String, Integer> notificationsByType) { this.notificationsByType = notificationsByType; }
        public Map<String, Integer> getNotificationsByCategory() { return notificationsByCategory; }
        public void setNotificationsByCategory(Map<String, Integer> notificationsByCategory) { this.notificationsByCategory = notificationsByCategory; }
        public Map<String, Integer> getDailyNotificationCount() { return dailyNotificationCount; }
        public void setDailyNotificationCount(Map<String, Integer> dailyNotificationCount) { this.dailyNotificationCount = dailyNotificationCount; }
    }

    public static class NotificationTemplateResponse {
        private String templateId;
        private String name;
        private String description;
        private String type;
        private String titleTemplate;
        private String messageTemplate;
        private Map<String, String> variables;
        private boolean isActive;
        
        // getters and setters
        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getTitleTemplate() { return titleTemplate; }
        public void setTitleTemplate(String titleTemplate) { this.titleTemplate = titleTemplate; }
        public String getMessageTemplate() { return messageTemplate; }
        public void setMessageTemplate(String messageTemplate) { this.messageTemplate = messageTemplate; }
        public Map<String, String> getVariables() { return variables; }
        public void setVariables(Map<String, String> variables) { this.variables = variables; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
    }
} 