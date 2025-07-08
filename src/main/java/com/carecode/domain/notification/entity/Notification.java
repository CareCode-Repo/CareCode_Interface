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
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType; // POLICY, HEALTH, COMMUNITY, SYSTEM
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private NotificationPriority priority; // HIGH, MEDIUM, LOW
    
    @Column(name = "is_read")
    private Boolean isRead;
    
    @Column(name = "is_sent")
    private Boolean isSent;
    
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private NotificationTemplate notificationTemplate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id")
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