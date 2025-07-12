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
 * 
 * <p>사용자에게 전송되는 알림을 관리합니다.
 * 단순한 구조로 필수 기능만 포함합니다.</p>
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
    private NotificationType notificationType;
    
    @Column(name = "TITLE", nullable = false)
    private String title;
    
    @Column(name = "MESSAGE", columnDefinition = "TEXT", nullable = false)
    private String message;
    
    @Column(name = "IS_READ", nullable = false)
    private Boolean isRead = false;
    
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    /**
     * 알림 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
    }
    
    /**
     * 알림 타입 Enum
     */
    public enum NotificationType {
        POLICY("정책"),
        HEALTH("건강"),
        COMMUNITY("커뮤니티"),
        SYSTEM("시스템");
        
        private final String displayName;
        
        NotificationType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
} 