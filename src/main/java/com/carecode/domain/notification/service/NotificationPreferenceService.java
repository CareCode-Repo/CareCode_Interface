package com.carecode.domain.notification.service;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.notification.dto.NotificationPreferenceDto;
import com.carecode.domain.notification.entity.NotificationPreference;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.repository.NotificationPreferenceRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 알림 설정 서비스 클래스
 * 사용자별 알림 설정을 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    /**
     * 사용자별 알림 설정 목록 조회
     */
    @LogExecutionTime
    public List<NotificationPreferenceDto> getUserPreferences(String userId) {
        log.info("사용자별 알림 설정 조회: 사용자ID={}", userId);
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new CareServiceException("사용자를 찾을 수 없습니다: " + userId));
            
            List<NotificationPreference> preferences = preferenceRepository.findByUserOrderByNotificationType(user);
            
            return preferences.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("사용자별 알림 설정 조회 실패: {}", e.getMessage(), e);
            throw new CareServiceException("알림 설정 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 알림 타입 설정 조회
     */
    @LogExecutionTime
    public NotificationPreferenceDto getPreferenceByType(String userId, Notification.NotificationType notificationType) {
        log.info("알림 타입별 설정 조회: 사용자ID={}, 타입={}", userId, notificationType);
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new CareServiceException("사용자를 찾을 수 없습니다: " + userId));
            
            Optional<NotificationPreference> preference = preferenceRepository.findByUserAndNotificationType(user, notificationType);
            
            return preference.map(this::convertToDto)
                    .orElseGet(() -> convertToDto(createDefaultPreference(user, notificationType)));
        } catch (Exception e) {
            log.error("알림 타입별 설정 조회 실패: {}", e.getMessage(), e);
            throw new CareServiceException("알림 설정 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 알림 설정 생성 또는 업데이트
     */
    @LogExecutionTime
    @Transactional
    public NotificationPreferenceDto savePreference(String userId, NotificationPreferenceDto preferenceDto) {
        log.info("알림 설정 저장: 사용자ID={}, 타입={}", userId, preferenceDto.getNotificationType());
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new CareServiceException("사용자를 찾을 수 없습니다: " + userId));
            
            NotificationPreference preference = preferenceRepository
                    .findByUserAndNotificationType(user, Notification.NotificationType.valueOf(preferenceDto.getNotificationType()))
                    .orElseGet(() -> createNewPreference(user, preferenceDto));
            
            // 설정 업데이트
            updatePreference(preference, preferenceDto);
            
            NotificationPreference savedPreference = preferenceRepository.save(preference);
            return convertToDto(savedPreference);
        } catch (Exception e) {
            log.error("알림 설정 저장 실패: {}", e.getMessage(), e);
            throw new CareServiceException("알림 설정 저장 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 채널별 설정 업데이트
     */
    @LogExecutionTime
    @Transactional
    public NotificationPreferenceDto updateChannelPreference(String userId, String notificationType, String channel, boolean enabled) {
        log.info("채널별 설정 업데이트: 사용자ID={}, 타입={}, 채널={}, 활성화={}", userId, notificationType, channel, enabled);
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new CareServiceException("사용자를 찾을 수 없습니다: " + userId));
            
            NotificationPreference preference = preferenceRepository
                    .findByUserAndNotificationType(user, Notification.NotificationType.valueOf(notificationType))
                    .orElseGet(() -> createDefaultPreference(user, Notification.NotificationType.valueOf(notificationType)));
            
            // 채널별 설정 업데이트
            updateChannelSetting(preference, channel, enabled);
            
            NotificationPreference savedPreference = preferenceRepository.save(preference);
            return convertToDto(savedPreference);
        } catch (Exception e) {
            log.error("채널별 설정 업데이트 실패: {}", e.getMessage(), e);
            throw new CareServiceException("채널별 설정 업데이트 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 모든 알림 설정 비활성화
     */
    @LogExecutionTime
    @Transactional
    public void disableAllNotifications(String userId) {
        log.info("모든 알림 설정 비활성화: 사용자ID={}", userId);
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new CareServiceException("사용자를 찾을 수 없습니다: " + userId));
            
            List<NotificationPreference> preferences = preferenceRepository.findByUserOrderByNotificationType(user);
            
            for (NotificationPreference preference : preferences) {
                preference.setEmailEnabled(false);
                preference.setPushEnabled(false);
                preference.setSmsEnabled(false);
                preference.setInAppEnabled(false);
                preferenceRepository.save(preference);
            }
        } catch (Exception e) {
            log.error("모든 알림 설정 비활성화 실패: {}", e.getMessage(), e);
            throw new CareServiceException("알림 설정 비활성화 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 기본 설정으로 초기화
     */
    @LogExecutionTime
    @Transactional
    public void resetToDefault(String userId) {
        log.info("알림 설정 기본값으로 초기화: 사용자ID={}", userId);
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new CareServiceException("사용자를 찾을 수 없습니다: " + userId));
            
            // 기존 설정 삭제
            List<NotificationPreference> existingPreferences = preferenceRepository.findByUserOrderByNotificationType(user);
            preferenceRepository.deleteAll(existingPreferences);
            
            // 기본 설정 생성
            for (Notification.NotificationType type : Notification.NotificationType.values()) {
                createDefaultPreference(user, type);
            }
        } catch (Exception e) {
            log.error("알림 설정 초기화 실패: {}", e.getMessage(), e);
            throw new CareServiceException("알림 설정 초기화 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 알림 타입의 활성화된 설정 조회
     */
    @LogExecutionTime
    public List<NotificationPreferenceDto> getEnabledPreferencesByType(Notification.NotificationType notificationType) {
        log.info("알림 타입별 활성화된 설정 조회: 타입={}", notificationType);
        
        try {
            List<NotificationPreference> preferences = preferenceRepository.findEnabledByNotificationType(notificationType);
            
            return preferences.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("알림 타입별 활성화된 설정 조회 실패: {}", e.getMessage(), e);
            throw new CareServiceException("활성화된 설정 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 기본 설정 생성
     */
    private NotificationPreference createDefaultPreference(User user, Notification.NotificationType notificationType) {
        NotificationPreference preference = NotificationPreference.builder()
                .user(user)
                .notificationType(notificationType)
                .emailEnabled(true)
                .pushEnabled(true)
                .smsEnabled(false)
                .inAppEnabled(true)
                .emailAddress(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
        
        return preferenceRepository.save(preference);
    }

    /**
     * 새 설정 생성
     */
    private NotificationPreference createNewPreference(User user, NotificationPreferenceDto preferenceDto) {
        return NotificationPreference.builder()
                .user(user)
                .notificationType(Notification.NotificationType.valueOf(preferenceDto.getNotificationType()))
                .emailEnabled(preferenceDto.getEmailEnabled())
                .pushEnabled(preferenceDto.getPushEnabled())
                .smsEnabled(preferenceDto.getSmsEnabled())
                .inAppEnabled(preferenceDto.getInAppEnabled())
                .emailAddress(preferenceDto.getEmailAddress())
                .phoneNumber(preferenceDto.getPhoneNumber())
                .deviceToken(preferenceDto.getDeviceToken())
                .build();
    }

    /**
     * 설정 업데이트
     */
    private void updatePreference(NotificationPreference preference, NotificationPreferenceDto preferenceDto) {
        preference.setEmailEnabled(preferenceDto.getEmailEnabled());
        preference.setPushEnabled(preferenceDto.getPushEnabled());
        preference.setSmsEnabled(preferenceDto.getSmsEnabled());
        preference.setInAppEnabled(preferenceDto.getInAppEnabled());
        preference.setEmailAddress(preferenceDto.getEmailAddress());
        preference.setPhoneNumber(preferenceDto.getPhoneNumber());
        preference.setDeviceToken(preferenceDto.getDeviceToken());
    }

    /**
     * 채널별 설정 업데이트
     */
    private void updateChannelSetting(NotificationPreference preference, String channel, boolean enabled) {
        switch (channel.toLowerCase()) {
            case "email" -> preference.setEmailEnabled(enabled);
            case "push" -> preference.setPushEnabled(enabled);
            case "sms" -> preference.setSmsEnabled(enabled);
            case "inapp" -> preference.setInAppEnabled(enabled);
            default -> throw new IllegalArgumentException("지원하지 않는 채널입니다: " + channel);
        }
    }

    /**
     * DTO 변환
     */
    private NotificationPreferenceDto convertToDto(NotificationPreference preference) {
        return NotificationPreferenceDto.builder()
                .id(preference.getId())
                .userId(preference.getUser().getUserId())
                .notificationType(preference.getNotificationType().name())
                .emailEnabled(preference.getEmailEnabled())
                .pushEnabled(preference.getPushEnabled())
                .smsEnabled(preference.getSmsEnabled())
                .inAppEnabled(preference.getInAppEnabled())
                .emailAddress(preference.getEmailAddress())
                .phoneNumber(preference.getPhoneNumber())
                .deviceToken(preference.getDeviceToken())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
} 