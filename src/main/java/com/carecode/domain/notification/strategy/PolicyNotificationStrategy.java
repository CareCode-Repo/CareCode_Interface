package com.carecode.domain.notification.strategy;

import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 정책 알림 전략
 * 육아 정책 관련 알림을 처리하는 전략
 */
@Slf4j
@Component
public class PolicyNotificationStrategy implements NotificationStrategy {
    
    @Override
    public String getNotificationType() {
        return "POLICY";
    }
    
    @Override
    public Notification createNotification(User user, String title, String message) {
        log.info("정책 알림 생성: 사용자={}, 제목={}", user.getEmail(), title);
        
        return Notification.builder()
                .user(user)
                .notificationType(Notification.NotificationType.POLICY)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
    }
    
    @Override
    public void processNotification(Notification notification) {
        log.info("정책 알림 처리: 알림ID={}, 제목={}", notification.getId(), notification.getTitle());
        
        // 정책 알림은 사용자 설정에 따라 전송
        // 실제로는 사용자의 정책 알림 설정을 확인하여 전송
        log.info("정책 알림 전송 완료: {}", notification.getTitle());
    }
    
    @Override
    public boolean validateNotification(Notification notification) {
        // 정책 알림은 정책 관련 키워드가 포함되어야 함
        String title = notification.getTitle().toLowerCase();
        String message = notification.getMessage().toLowerCase();
        
        return notification.getUser() != null && 
               notification.getTitle() != null && 
               !notification.getTitle().trim().isEmpty() &&
               notification.getMessage() != null &&
               !notification.getMessage().trim().isEmpty() &&
               (title.contains("정책") || title.contains("policy") || 
                message.contains("정책") || message.contains("policy"));
    }
    
    @Override
    public String determinePriority(Notification notification) {
        // 정책 알림은 기본적으로 중간 우선순위
        String title = notification.getTitle().toLowerCase();
        String message = notification.getMessage().toLowerCase();
        
        if (title.contains("긴급") || title.contains("urgent") || 
            message.contains("긴급") || message.contains("urgent")) {
            return "HIGH";
        } else if (title.contains("신규") || title.contains("new") || 
                   message.contains("신규") || message.contains("new")) {
            return "NORMAL";
        } else {
            return "LOW";
        }
    }
} 