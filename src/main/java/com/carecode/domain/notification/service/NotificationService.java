package com.carecode.domain.notification.service;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.domain.notification.dto.NotificationDto;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.repository.NotificationRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 알림 서비스 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    
    /**
     * 사용자 알림 목록 조회
     */
    @LogExecutionTime
    public List<NotificationDto> getUserNotifications(Long userId, int page, int size) {
        log.info("사용자 알림 목록 조회 - 사용자 ID: {}", userId);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        return notifications.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 알림 상세 조회
     */
    @LogExecutionTime
    public NotificationDto getNotificationById(Long notificationId) {
        log.info("알림 상세 조회 - 알림 ID: {}", notificationId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다: " + notificationId));
        
        return convertToDto(notification);
    }
    
    /**
     * 알림 생성
     */
    @Transactional
    public NotificationDto createNotification(NotificationDto request) {
        log.info("알림 생성 - 사용자 ID: {}, 타입: {}", request.getUserId(), request.getType());
        
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + request.getUserId()));
        
        Notification notification = Notification.builder()
                .user(user)
                .title(request.getTitle())
                .message(request.getContent())
                .notificationType(Notification.NotificationType.valueOf(request.getType()))
                .isRead(false)
                .build();
        
        Notification savedNotification = notificationRepository.save(notification);
        return convertToDto(savedNotification);
    }
    
    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        log.info("알림 읽음 처리 - 알림 ID: {}", notificationId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다: " + notificationId));
        
        notification.markAsRead();
        notificationRepository.save(notification);
    }
    
    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        log.info("모든 알림 읽음 처리 - 사용자 ID: {}", userId);
        
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        unreadNotifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unreadNotifications);
    }
    
    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        log.info("알림 삭제 - 알림 ID: {}", notificationId);
        
        notificationRepository.deleteById(notificationId);
    }
    
    /**
     * 읽지 않은 알림 개수 조회
     */
    @LogExecutionTime
    public long getUnreadCount(Long userId) {
        log.info("읽지 않은 알림 개수 조회 - 사용자 ID: {}", userId);
        
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
    
    /**
     * 알림 전송 (시스템 알림)
     */
    @Transactional
    public void sendSystemNotification(Long userId, String title, String content, String type) {
        log.info("시스템 알림 전송 - 사용자 ID: {}, 제목: {}, 타입: {}", userId, title, type);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(content)
                .notificationType(Notification.NotificationType.valueOf(type))
                .isRead(false)
                .build();
        
        notificationRepository.save(notification);
    }
    
    /**
     * Entity를 DTO로 변환
     */
    private NotificationDto convertToDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId())
                .title(notification.getTitle())
                .content(notification.getMessage())
                .type(notification.getNotificationType().name())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
} 