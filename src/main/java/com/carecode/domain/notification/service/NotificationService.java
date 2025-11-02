package com.carecode.domain.notification.service;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.notification.dto.NotificationRequest;
import com.carecode.domain.notification.dto.NotificationResponse;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.factory.NotificationStrategyFactory;
import com.carecode.domain.notification.repository.NotificationRepository;
import com.carecode.domain.notification.strategy.NotificationStrategy;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 알림 서비스 클래스
 * 전략 패턴을 사용하여 알림 타입별로 다른 처리 로직 적용
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationStrategyFactory strategyFactory;
    
    /**
     * 사용자별 알림 목록 조회
     */
    @LogExecutionTime
    public List<NotificationResponse.Notification> getNotificationsByUserId(String userId) {
        log.info("사용자별 알림 목록 조회 - 사용자 ID: {}", userId);
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            
            List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            
            return notifications.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("사용자별 알림 목록 조회 실패: {}", e.getMessage());
            throw new CareServiceException("사용자별 알림 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 알림 상세 조회
     */
    @LogExecutionTime
    public NotificationResponse.Notification getNotificationById(Long notificationId) {
        log.info("알림 상세 조회 - 알림 ID: {}", notificationId);
        
        try {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다: " + notificationId));
            
            return convertToResponseDto(notification);
        } catch (Exception e) {
            log.error("알림 상세 조회 실패: {}", e.getMessage());
            throw new CareServiceException("알림 상세 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 알림 생성 (전략 패턴 사용)
     */
    @Transactional
    public NotificationResponse.Notification createNotification(NotificationRequest.Create request) {
        log.info("알림 생성 - 사용자 ID: {}, 타입: {}, 제목: {}", 
                request.getUserId(), request.getNotificationType(), request.getTitle());
        
        try {
            User user = userRepository.findByUserId(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + request.getUserId()));
            
            // 전략 패턴을 사용하여 알림 생성
            NotificationStrategy strategy = strategyFactory.getStrategy(request.getNotificationType());
            Notification notification = strategy.createNotification(user, request.getTitle(), request.getMessage());
            
            // 알림 유효성 검사
            if (!strategy.validateNotification(notification)) {
                throw new IllegalArgumentException("알림 유효성 검사에 실패했습니다.");
            }
            
            // 알림 저장
            Notification savedNotification = notificationRepository.save(notification);
            
            // 알림 처리 (전송 등)
            strategy.processNotification(savedNotification);
            
            return convertToResponseDto(savedNotification);
        } catch (Exception e) {
            log.error("알림 생성 실패: {}", e.getMessage());
            throw new CareServiceException("알림 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 알림 수정
     */
    @Transactional
    public NotificationResponse.Notification updateNotification(Long notificationId, NotificationRequest.Create request) {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다: " + notificationId));
            
            // 전략을 사용하여 알림 수정
            NotificationStrategy strategy = strategyFactory.getStrategy(request.getNotificationType());
            
            notification.setNotificationType(Notification.NotificationType.valueOf(request.getNotificationType()));
            notification.setTitle(request.getTitle());
            notification.setMessage(request.getMessage());
            
            // 유효성 검사
            if (!strategy.validateNotification(notification)) {
                throw new IllegalArgumentException("알림 유효성 검사에 실패했습니다.");
            }
            
            Notification updatedNotification = notificationRepository.save(notification);
            return convertToResponseDto(updatedNotification);
    }

    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다: " + notificationId));

        notificationRepository.delete(notification);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId) {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다: " + notificationId));
            
            notification.markAsRead();
            notificationRepository.save(notification);
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead(String userId) {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            
            List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(user.getId());
            unreadNotifications.forEach(Notification::markAsRead);
            notificationRepository.saveAll(unreadNotifications);
    }

    /**
     * 읽지 않은 알림 조회
     */
    @LogExecutionTime
    public List<NotificationResponse.Notification> getUnreadNotifications(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(user.getId());

        return unreadNotifications.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 알림 설정 조회
     */
    @LogExecutionTime
    public Map<String, Object> getNotificationSettings(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        Map<String, Object> settings = new HashMap<>();
        settings.put("userId", userId);
        settings.put("emailEnabled", true);
        settings.put("pushEnabled", true);
        settings.put("smsEnabled", false);
        settings.put("enabledTypes", strategyFactory.getSupportedNotificationTypes());
        settings.put("quietHoursStart", "22:00");
        settings.put("quietHoursEnd", "08:00");

        return settings;
    }

    /**
     * 알림 설정 업데이트
     */
    @Transactional
    public Map<String, Object> updateNotificationSettings(String userId, Map<String, Object> settings) {
        log.info("알림 설정 업데이트 - 사용자 ID: {}", userId);
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            
            // 실제로는 NotificationSettings 엔티티를 업데이트
            Map<String, Object> updatedSettings = new HashMap<>(settings);
            updatedSettings.put("userId", userId);
            updatedSettings.put("updatedAt", LocalDateTime.now().toString());
            
            return updatedSettings;
        } catch (Exception e) {
            log.error("알림 설정 업데이트 실패: {}", e.getMessage());
            throw new CareServiceException("알림 설정 업데이트 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 알림 통계 조회
     */
    @LogExecutionTime
    public Map<String, Object> getNotificationStatistics(String userId) {
        log.info("알림 통계 조회 - 사용자 ID: {}", userId);
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            
            long totalNotifications = notificationRepository.countByUserId(user.getId());
            long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
            long readCount = totalNotifications - unreadCount;
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("userId", userId);
            statistics.put("totalNotifications", totalNotifications);
            statistics.put("unreadCount", unreadCount);
            statistics.put("readCount", readCount);
            statistics.put("typeDistribution", calculateTypeDistribution(user.getId()));
            statistics.put("priorityDistribution", Map.of("HIGH", 5, "NORMAL", 15, "LOW", 5));
            statistics.put("dailyNotificationCount", Map.of("2024-01-15", 3, "2024-01-16", 2));
            
            return statistics;
        } catch (Exception e) {
            log.error("알림 통계 조회 실패: {}", e.getMessage());
            throw new CareServiceException("알림 통계 조회 중 오류가 발생했습니다.", e);
        }
    }

    // 기존 메서드들 (호환성을 위해 유지)
    
    /**
     * 사용자 알림 목록 조회 (페이지네이션)
     */
    @LogExecutionTime
    public List<NotificationResponse.Notification> getUserNotifications(Long userId, int page, int size) {
        log.info("사용자 알림 목록 조회 - 사용자 ID: {}", userId);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        return notifications.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
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
     * 시스템 알림 발송 (전략 패턴 사용)
     */
    @Transactional
    public void sendSystemNotification(Long userId, String title, String content, String type) {
        log.info("시스템 알림 발송 - 사용자 ID: {}, 제목: {}", userId, title);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        NotificationStrategy strategy = strategyFactory.getStrategy(type);
        Notification notification = strategy.createNotification(user, title, content);
        
        if (strategy.validateNotification(notification)) {
            notificationRepository.save(notification);
            strategy.processNotification(notification);
        } else {
            throw new IllegalArgumentException("알림 유효성 검사에 실패했습니다.");
        }
    }

    // Helper methods
    private NotificationResponse.Notification convertToResponseDto(Notification notification) {
        // 전략을 사용하여 우선순위 결정
        NotificationStrategy strategy = strategyFactory.getStrategy(notification.getNotificationType().name());
        String priority = strategy.determinePriority(notification);
        
        return NotificationResponse.Notification.builder()
                .id(notification.getId())
                .userId(notification.getUser().getUserId()) // String 타입으로 변경
                .notificationType(notification.getNotificationType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .priority(priority)
                .isRead(notification.getIsRead())
                .scheduledAt(null) // 기본값
                .sentAt(notification.getCreatedAt())
                .readAt(null) // 읽음 처리 시 설정
                .createdAt(notification.getCreatedAt())
                .deliveryStatus("SENT") // 기본값
                .build();
    }

    /**
     * DTO 변환 메서드
     */
    private NotificationResponse.Notification convertToDto(Notification notification) {
        return NotificationResponse.Notification.builder()
                .id(notification.getId())
                .userId(notification.getUser().getUserId()) // String 타입으로 변경
                .title(notification.getTitle())
                .message(notification.getMessage()) // content -> message로 변경
                .notificationType(notification.getNotificationType().name()) // type -> notificationType으로 변경
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    /**
     * 알림 타입별 분포 계산
     */
    private Map<String, Integer> calculateTypeDistribution(Long userId) {
        Map<String, Integer> distribution = new HashMap<>();
        
        for (Notification.NotificationType type : Notification.NotificationType.values()) {
            long count = notificationRepository.countByUserIdAndNotificationType(userId, type);
            distribution.put(type.name(), (int) count);
        }
        
        return distribution;
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(NotificationRequest.MarkAsRead request) {
        if (request.getMarkAllAsRead() != null && request.getMarkAllAsRead()) {
            // 모든 알림을 읽음으로 처리
            notificationRepository.markAllAsRead();
        } else if (request.getNotificationIds() != null && !request.getNotificationIds().isEmpty()) {
            // 특정 알림들을 읽음으로 처리
            notificationRepository.markAsReadByIds(request.getNotificationIds());
        }
    }

    /**
     * 테스트 알림 발송
     */
    @Transactional
    public void sendTestNotification(String userId, NotificationRequest.SendTest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CareServiceException("사용자를 찾을 수 없습니다: " + userId));

        Notification notification = Notification.builder()
                .user(user)
                .title(request.getTitle())
                .message(request.getMessage())
                .notificationType(Notification.NotificationType.valueOf(request.getType()))
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    /**
     * 알림 통계 조회
     */
    @Transactional(readOnly = true)
    public NotificationResponse.Stats getNotificationStats(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CareServiceException("사용자를 찾을 수 없습니다: " + userId));

        long totalCount = notificationRepository.countByUserId(user.getId());
        long unreadCount = notificationRepository.countByUserIdAndIsRead(user.getId(), false);
        long readCount = totalCount - unreadCount;

        Map<String, Integer> typeDistribution = calculateTypeDistribution(user.getId());
        Map<String, Integer> priorityDistribution = new HashMap<>();
        Map<String, Integer> dailyNotificationCount = new HashMap<>();

        return NotificationResponse.Stats.builder()
                .totalNotifications((int) totalCount)
                .unreadCount((int) unreadCount)
                .readCount((int) readCount)
                .typeDistribution(typeDistribution)
                .priorityDistribution(priorityDistribution)
                .dailyNotificationCount(dailyNotificationCount)
                .build();
    }

    /**
     * 알림 템플릿 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse.Template> getNotificationTemplates(String type) {
        List<NotificationResponse.Template> templates = new ArrayList<>();
        
        // 기본 템플릿들
        templates.add(NotificationResponse.Template.builder()
                .templateId("SYSTEM_UPDATE")
                .templateName("시스템 업데이트")
                .notificationType("SYSTEM")
                .title("시스템 업데이트 알림")
                .message("새로운 기능이 추가되었습니다.")
                .description("시스템 업데이트 관련 알림")
                .isActive(true)
                .build());

        templates.add(NotificationResponse.Template.builder()
                .templateId("POLICY_CHANGE")
                .templateName("정책 변경")
                .notificationType("POLICY")
                .title("정책 변경 알림")
                .message("새로운 정책이 적용되었습니다.")
                .description("정책 변경 관련 알림")
                .isActive(true)
                .build());

        if (type != null && !type.isEmpty()) {
            return templates.stream()
                    .filter(template -> template.getNotificationType().equals(type))
                    .collect(Collectors.toList());
        }

        return templates;
    }

    /**
     * 알림 전송 상태 조회
     */
    @Transactional(readOnly = true)
    public NotificationResponse.DeliveryStatus getDeliveryStatus(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CareServiceException("알림을 찾을 수 없습니다: " + notificationId));

        return NotificationResponse.DeliveryStatus.builder()
                .notificationId(notificationId)
                .deliveryStatus("SENT")
                .deliveryMethod("IN_APP")
                .sentAt(notification.getCreatedAt().toString())
                .deliveredAt(notification.getCreatedAt().toString())
                .errorMessage(null)
                .build();
    }
} 