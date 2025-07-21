package com.carecode.domain.notification.strategy;

import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 시스템 알림 전략
 * 시스템 관련 알림을 처리하는 전략
 */
@Slf4j
@Component
public class SystemNotificationStrategy implements NotificationStrategy {
    
    @Override
    public String getNotificationType() {
        return "SYSTEM";
    }
    
    @Override
    public Notification createNotification(User user, String title, String message) {
        log.info("시스템 알림 생성: 사용자={}, 제목={}", user.getEmail(), title);
        
        return Notification.builder()
                .user(user)
                .notificationType(Notification.NotificationType.SYSTEM)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
    }
    
    @Override
    public void processNotification(Notification notification) {
        log.info("시스템 알림 처리: 알림ID={}, 제목={}", notification.getId(), notification.getTitle());
        
        // 시스템 알림은 즉시 전송
        // 실제로는 이메일, 푸시 알림 등을 발송
        log.info("시스템 알림 전송 완료: {}", notification.getTitle());
    }
    
    @Override
    public boolean validateNotification(Notification notification) {
        // 시스템 알림은 관리자만 생성 가능하도록 검증
        return notification.getUser() != null && 
               notification.getTitle() != null && 
               !notification.getTitle().trim().isEmpty() &&
               notification.getMessage() != null &&
               !notification.getMessage().trim().isEmpty();
    }
    
    @Override
    public String determinePriority(Notification notification) {
        // 시스템 알림은 기본적으로 높은 우선순위
        String title = notification.getTitle().toLowerCase();
        
        if (title.contains("긴급") || title.contains("urgent") || title.contains("error")) {
            return "HIGH";
        } else if (title.contains("업데이트") || title.contains("update")) {
            return "NORMAL";
        } else {
            return "LOW";
        }
    }
} 