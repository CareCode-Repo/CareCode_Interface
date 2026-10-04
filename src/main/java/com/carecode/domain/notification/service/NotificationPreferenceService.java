package com.carecode.domain.notification.service;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.exception.CareServiceException;
import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.domain.notification.dto.request.NotificationRegisterPushTokenRequest;
import com.carecode.domain.notification.dto.request.NotificationUpdateSettingsRequest;
import com.carecode.domain.notification.dto.response.NotificationSettingsResponse;
import com.carecode.domain.notification.entity.NotificationPreference;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.repository.NotificationPreferenceRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.carecode.domain.notification.entity.PushDevice;
import com.carecode.domain.notification.repository.PushDeviceRepository;

/** 알림 설정 서비스 클래스 사용자별 알림 설정을 관리 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final PushDeviceRepository pushDeviceRepository;
    private final UserRepository userRepository;

    // 사용자별 알림 설정 목록 조회
    @LogExecutionTime
    public List<NotificationSettingsResponse> getUserPreferences(String userId) {
        log.info("사용자별 알림 설정 조회: 사용자ID={}", userId);
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            
            List<NotificationPreference> preferences = preferenceRepository.findByUserOrderByNotificationType(user);
            
            return preferences.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("사용자별 알림 설정 조회 실패: {}", e.getMessage(), e);
            throw new CareServiceException("알림 설정 조회 중 오류가 발생했습니다.", e);
        }
    }

    // 특정 알림 타입 설정 조회
    @LogExecutionTime
    public NotificationSettingsResponse getPreferenceByType(String userId, Notification.NotificationType notificationType) {
        log.info("알림 타입별 설정 조회: 사용자ID={}, 타입={}", userId, notificationType);
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            
            Optional<NotificationPreference> preference = preferenceRepository.findByUserAndNotificationType(user, notificationType);
            
            return preference.map(this::convertToDto)
                    .orElseGet(() -> convertToDto(createDefaultPreference(user, notificationType)));
        } catch (Exception e) {
            log.error("알림 타입별 설정 조회 실패: {}", e.getMessage(), e);
            throw new CareServiceException("알림 설정 조회 중 오류가 발생했습니다.", e);
        }
    }

    // 알림 설정 생성 또는 업데이트
    @LogExecutionTime
    @Transactional
    public NotificationSettingsResponse savePreference(String userId, NotificationSettingsResponse preferenceDto) {
        log.info("알림 설정 저장: 사용자ID={}, 타입={}", userId, preferenceDto.getNotificationType());
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            
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

    // 채널별 설정 업데이트
    @LogExecutionTime
    @Transactional
    public NotificationSettingsResponse updateChannelPreference(String userId, String notificationType, String channel, boolean enabled) {
        log.info("채널별 설정 업데이트: 사용자ID={}, 타입={}, 채널={}, 활성화={}", userId, notificationType, channel, enabled);
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            
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
     * 모든 알림 설정 비활성화.
     *
     * 설정 행이 없는 유형도 함께 끈다. 저장된 행만 끄면, 설정을 한 번도 건드린 적 없는 사용자는
     * "모두 끄기" 를 눌러도 행이 없어 아무것도 바뀌지 않고 인앱·푸시 기본값으로 계속 알림을 받는다.
     */
    @LogExecutionTime
    @Transactional
    public void disableAllNotifications(String userId) {
        log.info("모든 알림 설정 비활성화: 사용자ID={}", userId);
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            
            Map<Notification.NotificationType, NotificationPreference> stored =
                    preferenceRepository.findByUserOrderByNotificationType(user).stream()
                            .collect(Collectors.toMap(NotificationPreference::getNotificationType, preference -> preference, (a, b) -> a));

            for (Notification.NotificationType type : Notification.NotificationType.values()) {
                NotificationPreference preference = stored.containsKey(type)
                        ? stored.get(type)
                        : createDefaultPreference(user, type);

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

    // 기본 설정으로 초기화
    @LogExecutionTime
    @Transactional
    public void resetToDefault(String userId) {
        log.info("알림 설정 기본값으로 초기화: 사용자ID={}", userId);
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            
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

    // 특정 알림 타입의 활성화된 설정 조회
    @LogExecutionTime
    public List<NotificationSettingsResponse> getEnabledPreferencesByType(Notification.NotificationType notificationType) {
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
     * 기본 설정 생성.
     *
     * 기본값은 설정 행이 없을 때 {@code NotificationDispatcher} 가 실제로 발송하는 채널과 같아야 한다.
     * 예전에는 여기서만 이메일을 켜 두어, 사용자가 설정 화면에서 다른 채널 하나를 끄는 순간
     * (그 시점에 이 기본 행이 만들어지면서) 요청한 적 없는 이메일 알림이 켜졌다.
     */
    private NotificationPreference createDefaultPreference(User user, Notification.NotificationType notificationType) {
        NotificationPreference preference = NotificationPreference.builder()
                .user(user)
                .notificationType(notificationType)
                .emailEnabled(false)
                .pushEnabled(true)
                .smsEnabled(false)
                .inAppEnabled(true)
                .emailAddress(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
        
        return preferenceRepository.save(preference);
    }

    // 새 설정 생성
    private NotificationPreference createNewPreference(User user, NotificationSettingsResponse preferenceDto) {
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

    // 설정 업데이트
    private void updatePreference(NotificationPreference preference, NotificationSettingsResponse preferenceDto) {
        preference.setEmailEnabled(preferenceDto.getEmailEnabled());
        preference.setPushEnabled(preferenceDto.getPushEnabled());
        preference.setSmsEnabled(preferenceDto.getSmsEnabled());
        preference.setInAppEnabled(preferenceDto.getInAppEnabled());
        preference.setEmailAddress(preferenceDto.getEmailAddress());
        preference.setPhoneNumber(preferenceDto.getPhoneNumber());
        preference.setDeviceToken(preferenceDto.getDeviceToken());
    }

    // 채널별 설정 업데이트
    private void updateChannelSetting(NotificationPreference preference, String channel, boolean enabled) {
        switch (channel.toLowerCase()) {
            case "email" -> preference.setEmailEnabled(enabled);
            case "push" -> preference.setPushEnabled(enabled);
            case "sms" -> preference.setSmsEnabled(enabled);
            case "inapp" -> preference.setInAppEnabled(enabled);
            default -> throw new IllegalArgumentException("지원하지 않는 채널입니다: " + channel);
        }
    }

    // DTO 변환
    private NotificationSettingsResponse convertToDto(NotificationPreference preference) {
        return NotificationSettingsResponse.builder()
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

    // 푸시 알림 토큰 등록
    @Transactional
    public void registerPushToken(String userId, NotificationRegisterPushTokenRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 기존 설정이 있는지 확인
        Optional<NotificationPreference> existingPreference = preferenceRepository
                .findByUserAndNotificationType(user, Notification.NotificationType.SYSTEM);

        if (existingPreference.isPresent()) {
            NotificationPreference preference = existingPreference.get();
            preference.setDeviceToken(request.getPushToken());
            preference.setUpdatedAt(LocalDateTime.now());
            preferenceRepository.save(preference);
        } else {
            // 새 설정 생성
            NotificationPreference newPreference = NotificationPreference.builder()
                    .user(user)
                    .notificationType(Notification.NotificationType.SYSTEM)
                    .emailEnabled(false)
                    .pushEnabled(true)
                    .smsEnabled(false)
                    .inAppEnabled(true)
                    .deviceToken(request.getPushToken())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            preferenceRepository.save(newPreference);
        }

        registerDevice(user, request);
    }

    /**
     * 이 기기를 사용자의 푸시 대상으로 올린다.
     *
     * <p>위에서 알림 설정 행에도 토큰을 쓰지만, 그 칸은 사용자당 하나뿐이라 휴대폰에서 켜면
     * 웹에서 켜 둔 것을 덮어썼다. 기기는 여러 대일 수 있으므로 행을 따로 남긴다.
     * (설정 행 쪽은 건드리지 않는다 — 알림 켜짐/꺼짐이 그 행에 달려 있고, 예전에 등록해 둔
     * 사용자가 아직 그 값으로 알림을 받고 있다.)
     */
    private void registerDevice(User user, NotificationRegisterPushTokenRequest request) {
        LocalDateTime now = LocalDateTime.now();

        PushDevice device = pushDeviceRepository.findByToken(request.getPushToken())
                .orElseGet(() -> PushDevice.builder()
                        .token(request.getPushToken())
                        .createdAt(now)
                        .build());

        // 같은 기기를 다른 사람이 쓰기 시작했을 수 있다(기기를 넘겨주거나 계정을 바꿔 로그인).
        // 토큰은 전역 유일하므로 주인만 옮긴다 — 그러지 않으면 이전 사용자에게 알림이 계속 간다.
        device.setUser(user);
        device.setDeviceType(request.getDeviceType());
        device.setAppVersion(request.getAppVersion());
        device.setUpdatedAt(now);

        pushDeviceRepository.save(device);
    }

    // 알림 설정 수정
    @Transactional
    public void updateSettings(String userId, NotificationUpdateSettingsRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 각 알림 타입별로 설정 업데이트
        for (Notification.NotificationType type : Notification.NotificationType.values()) {
            Optional<NotificationPreference> existingPreference = preferenceRepository
                    .findByUserAndNotificationType(user, type);

            NotificationPreference preference;
            if (existingPreference.isPresent()) {
                preference = existingPreference.get();
            } else {
                preference = NotificationPreference.builder()
                        .user(user)
                        .notificationType(type)
                        .createdAt(LocalDateTime.now())
                        .build();
            }

            // 타입별 설정 적용
            switch (type) {
                case POLICY:
                    preference.setEmailEnabled(request.isEmailNotification());
                    preference.setPushEnabled(request.isPushNotification());
                    preference.setSmsEnabled(request.isSmsNotification());
                    preference.setInAppEnabled(request.isPolicyNotification());
                    break;
                case HEALTH:
                    preference.setEmailEnabled(request.isEmailNotification());
                    preference.setPushEnabled(request.isPushNotification());
                    preference.setSmsEnabled(request.isSmsNotification());
                    preference.setInAppEnabled(request.isFacilityNotification());
                    break;
                case COMMUNITY:
                    preference.setEmailEnabled(request.isEmailNotification());
                    preference.setPushEnabled(request.isPushNotification());
                    preference.setSmsEnabled(request.isSmsNotification());
                    preference.setInAppEnabled(request.isCommunityNotification());
                    break;
                case SYSTEM:
                    preference.setEmailEnabled(request.isEmailNotification());
                    preference.setPushEnabled(request.isPushNotification());
                    preference.setSmsEnabled(request.isSmsNotification());
                    preference.setInAppEnabled(request.isChatbotNotification());
                    break;
            }

            preference.setUpdatedAt(LocalDateTime.now());
            preferenceRepository.save(preference);
        }
    }
} 