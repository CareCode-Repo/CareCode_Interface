package com.carecode.domain.notification.entity;

import com.carecode.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 알림 엔티티
 */
@Entity
@Table(name = "TBL_NOTIFICATION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "NOTIFICATION_TYPE", nullable = false)
    private NotificationType notificationType; // POLICY, HEALTH, COMMUNITY, SYSTEM
    
    @Column(name = "TITLE", nullable = false)
    private String title;
    
    @Column(name = "MESSAGE", columnDefinition = "TEXT", nullable = false)
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "PRIORITY")
    private NotificationPriority priority; // HIGH, MEDIUM, LOW
    
    @Column(name = "IS_READ")
    private Boolean isRead;
    
    @Column(name = "IS_SENT")
    private Boolean isSent;
    
    @Column(name = "SCHEDULED_AT")
    private LocalDateTime scheduledAt;
    
    @Column(name = "SENT_AT")
    private LocalDateTime sentAt;
    
    @Column(name = "READ_AT")
    private LocalDateTime readAt;
    
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TEMPLATE_ID")
    private NotificationTemplate notificationTemplate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CHANNEL_ID")
    private NotificationChannel notificationChannel;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isRead == null) {
            isRead = false;
        }
        if (isSent == null) {
            isSent = false;
        }
        if (priority == null) {
            priority = NotificationPriority.MEDIUM;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 알림 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
    
    /**
     * 알림 전송 완료 처리
     */
    public void markAsSent() {
        this.isSent = true;
        this.sentAt = LocalDateTime.now();
    }
    
    /**
     * 알림 타입 Enum
     */
    public enum NotificationType {
        POLICY("정책"),
        HEALTH("건강"),
        COMMUNITY("커뮤니티"),
        SYSTEM("시스템"),
        FACILITY("시설");
        
        private final String displayName;
        
        NotificationType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 알림 우선순위 Enum
     */
    public enum NotificationPriority {
        HIGH("높음"),
        MEDIUM("보통"),
        LOW("낮음");
        
        private final String displayName;
        
        NotificationPriority(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
} 