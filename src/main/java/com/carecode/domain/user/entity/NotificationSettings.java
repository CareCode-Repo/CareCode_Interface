package com.carecode.domain.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * 알림 설정 엔티티
 * 사용자의 알림 설정을 관리
 */
@Entity
@Table(name = "TBL_NOTIFICATION_SETTINGS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false, unique = true)
    private User user;
    
    @Column(name = "POLICY_NOTIFICATION")
    private Boolean policyNotification;
    
    @Column(name = "FACILITY_NOTIFICATION")
    private Boolean facilityNotification;
    
    @Column(name = "COMMUNITY_NOTIFICATION")
    private Boolean communityNotification;
    
    @Column(name = "CHATBOT_NOTIFICATION")
    private Boolean chatbotNotification;
    
    @Column(name = "EMAIL_NOTIFICATION")
    private Boolean emailNotification;
    
    @Column(name = "PUSH_NOTIFICATION")
    private Boolean pushNotification;
    
    @Column(name = "SMS_NOTIFICATION")
    private Boolean smsNotification;
    
    @Column(name = "QUIET_HOURS_START")
    private LocalTime quietHoursStart;
    
    @Column(name = "QUIET_HOURS_END")
    private LocalTime quietHoursEnd;
    
    @Column(name = "QUIET_HOURS_ENABLED")
    private Boolean quietHoursEnabled;
    
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        setDefaultValues();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    private void setDefaultValues() {
        if (policyNotification == null) policyNotification = true;
        if (facilityNotification == null) facilityNotification = true;
        if (communityNotification == null) communityNotification = true;
        if (chatbotNotification == null) chatbotNotification = true;
        if (emailNotification == null) emailNotification = true;
        if (pushNotification == null) pushNotification = true;
        if (smsNotification == null) smsNotification = false;
        if (quietHoursEnabled == null) quietHoursEnabled = false;
    }
    
    // 비즈니스 로직 메서드들
    public boolean isNotificationEnabled(String notificationType) {
        switch (notificationType) {
            case "POLICY":
                return policyNotification != null && policyNotification;
            case "FACILITY":
                return facilityNotification != null && facilityNotification;
            case "COMMUNITY":
                return communityNotification != null && communityNotification;
            case "CHATBOT":
                return chatbotNotification != null && chatbotNotification;
            case "EMAIL":
                return emailNotification != null && emailNotification;
            case "PUSH":
                return pushNotification != null && pushNotification;
            case "SMS":
                return smsNotification != null && smsNotification;
            default:
                return false;
        }
    }
    
    public boolean isInQuietHours() {
        if (!quietHoursEnabled || quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }
        
        LocalTime now = LocalTime.now();
        if (quietHoursStart.isBefore(quietHoursEnd)) {
            return now.isAfter(quietHoursStart) && now.isBefore(quietHoursEnd);
        } else {
            // 자정을 넘어가는 경우
            return now.isAfter(quietHoursStart) || now.isBefore(quietHoursEnd);
        }
    }
    
    public boolean shouldSendNotification(String notificationType) {
        return isNotificationEnabled(notificationType) && !isInQuietHours();
    }
} 