package com.carecode.domain.notification.strategy;

import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 커뮤니티 알림 전략
 * 커뮤니티 활동 관련 알림을 처리하는 전략
 */
@Slf4j
@Component
public class CommunityNotificationStrategy implements NotificationStrategy {
    
    @Override
    public String getNotificationType() {
        return "COMMUNITY";
    }
    
    @Override
    public Notification createNotification(User user, String title, String message) {
        log.info("커뮤니티 알림 생성: 사용자={}, 제목={}", user.getEmail(), title);
        
        return Notification.builder()
                .user(user)
                .notificationType(Notification.NotificationType.COMMUNITY)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
    }
    
    @Override
    public void processNotification(Notification notification) {
        log.info("커뮤니티 알림 처리: 알림ID={}, 제목={}", notification.getId(), notification.getTitle());
        
        // 커뮤니티 알림은 사용자 설정에 따라 전송
        // 실제로는 사용자의 커뮤니티 알림 설정을 확인하여 전송
        log.info("커뮤니티 알림 전송 완료: {}", notification.getTitle());
    }
    
    @Override
    public boolean validateNotification(Notification notification) {
        // 커뮤니티 알림은 커뮤니티 관련 키워드가 포함되어야 함
        String title = notification.getTitle().toLowerCase();
        String message = notification.getMessage().toLowerCase();
        
        return notification.getUser() != null && 
               notification.getTitle() != null && 
               !notification.getTitle().trim().isEmpty() &&
               notification.getMessage() != null &&
               !notification.getMessage().trim().isEmpty() &&
               (title.contains("커뮤니티") || title.contains("community") || 
                message.contains("커뮤니티") || message.contains("community") ||
                title.contains("댓글") || title.contains("comment") ||
                message.contains("댓글") || message.contains("comment"));
    }
    
    @Override
    public String determinePriority(Notification notification) {
        // 커뮤니티 알림은 기본적으로 낮은 우선순위
        String title = notification.getTitle().toLowerCase();
        String message = notification.getMessage().toLowerCase();
        
        if (title.contains("댓글") || title.contains("comment") || 
            message.contains("댓글") || message.contains("comment")) {
            return "NORMAL";
        } else if (title.contains("팁") || title.contains("tip") || 
                   message.contains("팁") || message.contains("tip")) {
            return "NORMAL";
        } else {
            return "LOW";
        }
    }
} 