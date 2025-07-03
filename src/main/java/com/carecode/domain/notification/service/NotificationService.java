package com.carecode.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 알림 서비스 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    /**
     * 사용자 알림 목록 조회
     */
    public List<Object> getUserNotifications(String userId) {
        log.info("사용자 알림 목록 조회 - 사용자 ID: {}", userId);
        // 실제로는 데이터베이스에서 조회
        return List.of();
    }
    
    /**
     * 알림 전송
     */
    public void sendNotification(String userId, String message, String type) {
        log.info("알림 전송 - 사용자 ID: {}, 메시지: {}, 타입: {}", userId, message, type);
        // 실제로는 알림 시스템과 연동
    }
} 