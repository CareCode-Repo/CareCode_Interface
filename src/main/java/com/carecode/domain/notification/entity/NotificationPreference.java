package com.carecode.domain.notification.entity;

import com.carecode.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 알림 설정 엔티티
 * 사용자별 알림 수신 설정을 관리
 */
@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Notification.NotificationType notificationType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pushEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean smsEnabled = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean inAppEnabled = true;

    @Column
    private String emailAddress;

    @Column
    private String phoneNumber;

    @Column
    private String deviceToken;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 모든 채널이 비활성화되어 있는지 확인
     */
    public boolean isAllChannelsDisabled() {
        return !emailEnabled && !pushEnabled && !smsEnabled && !inAppEnabled;
    }

    /**
     * 특정 채널이 활성화되어 있는지 확인
     */
    public boolean isChannelEnabled(String channel) {
        return switch (channel.toLowerCase()) {
            case "email" -> emailEnabled;
            case "push" -> pushEnabled;
            case "sms" -> smsEnabled;
            case "inapp" -> inAppEnabled;
            default -> false;
        };
    }
} 