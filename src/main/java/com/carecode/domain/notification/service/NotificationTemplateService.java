package com.carecode.domain.notification.service;

import com.carecode.domain.notification.dto.NotificationRequestDto;
import com.carecode.domain.notification.factory.NotificationStrategyFactory;
import com.carecode.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 알림 템플릿 서비스
 * 공통 알림 템플릿을 관리하고 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTemplateService {
    
    private final NotificationStrategyFactory strategyFactory;
    
    /**
     * 시스템 업데이트 알림 템플릿
     */
    public NotificationRequestDto.CreateNotificationRequest createSystemUpdateTemplate(User user, String version, String features) {
        return NotificationRequestDto.CreateNotificationRequest.builder()
                .userId(user.getUserId())
                .notificationType("SYSTEM")
                .title("시스템 업데이트 알림")
                .message(String.format("새로운 버전 %s이(가) 업데이트되었습니다. 새로운 기능: %s", version, features))
                .priority("NORMAL")
                .build();
    }
    
    /**
     * 정책 변경 알림 템플릿
     */
    public NotificationRequestDto.CreateNotificationRequest createPolicyChangeTemplate(User user, String policyName, String changeDetails) {
        return NotificationRequestDto.CreateNotificationRequest.builder()
                .userId(user.getUserId())
                .notificationType("POLICY")
                .title("육아 정책 변경 알림")
                .message(String.format("정책 '%s'이(가) 변경되었습니다. 변경 내용: %s", policyName, changeDetails))
                .priority("HIGH")
                .build();
    }
    
    /**
     * 커뮤니티 활동 알림 템플릿
     */
    public NotificationRequestDto.CreateNotificationRequest createCommunityActivityTemplate(User user, String activityType, String content) {
        return NotificationRequestDto.CreateNotificationRequest.builder()
                .userId(user.getUserId())
                .notificationType("COMMUNITY")
                .title("커뮤니티 활동 알림")
                .message(String.format("새로운 %s이(가) 있습니다: %s", activityType, content))
                .priority("LOW")
                .build();
    }
    
    /**
     * 건강 기록 알림 템플릿
     */
    public NotificationRequestDto.CreateNotificationRequest createHealthRecordTemplate(User user, String childName, String recordType) {
        return NotificationRequestDto.CreateNotificationRequest.builder()
                .userId(user.getUserId())
                .notificationType("HEALTH")
                .title("건강 기록 알림")
                .message(String.format("%s의 %s 기록을 확인해주세요.", childName, recordType))
                .priority("NORMAL")
                .build();
    }
    
    /**
     * 예방접종 알림 템플릿
     */
    public NotificationRequestDto.CreateNotificationRequest createVaccinationReminderTemplate(User user, String childName, String vaccineName, String dueDate) {
        return NotificationRequestDto.CreateNotificationRequest.builder()
                .userId(user.getUserId())
                .notificationType("HEALTH")
                .title("예방접종 알림")
                .message(String.format("%s의 %s 예방접종이 %s에 예정되어 있습니다.", childName, vaccineName, dueDate))
                .priority("HIGH")
                .build();
    }
    
    /**
     * 시설 추천 알림 템플릿
     */
    public NotificationRequestDto.CreateNotificationRequest createFacilityRecommendationTemplate(User user, String facilityName, String reason) {
        return NotificationRequestDto.CreateNotificationRequest.builder()
                .userId(user.getUserId())
                .notificationType("FACILITY")
                .title("시설 추천 알림")
                .message(String.format("새로운 육아 시설 '%s'이(가) 추천됩니다. 이유: %s", facilityName, reason))
                .priority("NORMAL")
                .build();
    }
    
    /**
     * 챗봇 응답 알림 템플릿
     */
    public NotificationRequestDto.CreateNotificationRequest createChatbotResponseTemplate(User user, String question, String answer) {
        return NotificationRequestDto.CreateNotificationRequest.builder()
                .userId(user.getUserId())
                .notificationType("CHATBOT")
                .title("챗봇 응답 알림")
                .message(String.format("질문: %s\n답변: %s", question, answer))
                .priority("LOW")
                .build();
    }
    
    /**
     * 긴급 알림 템플릿
     */
    public NotificationRequestDto.CreateNotificationRequest createEmergencyTemplate(User user, String emergencyType, String details) {
        return NotificationRequestDto.CreateNotificationRequest.builder()
                .userId(user.getUserId())
                .notificationType("SYSTEM")
                .title("긴급 알림")
                .message(String.format("긴급 상황: %s - %s", emergencyType, details))
                .priority("HIGH")
                .build();
    }
    
    /**
     * 사용자 정의 알림 템플릿 생성
     */
    public NotificationRequestDto.CreateNotificationRequest createCustomTemplate(User user, String type, String title, String message, String priority) {
        return NotificationRequestDto.CreateNotificationRequest.builder()
                .userId(user.getUserId())
                .notificationType(type)
                .title(title)
                .message(message)
                .priority(priority)
                .build();
    }
    
    /**
     * 템플릿 유효성 검사
     */
    public boolean validateTemplate(NotificationRequestDto.CreateNotificationRequest template) {
        if (template == null) return false;
        
        // 지원하는 알림 타입인지 확인
        if (!strategyFactory.supportsNotificationType(template.getNotificationType())) {
            log.warn("지원하지 않는 알림 타입: {}", template.getNotificationType());
            return false;
        }
        
        // 필수 필드 검사
        if (template.getUserId() == null || template.getUserId().trim().isEmpty()) {
            log.warn("사용자 ID가 없습니다.");
            return false;
        }
        
        if (template.getTitle() == null || template.getTitle().trim().isEmpty()) {
            log.warn("알림 제목이 없습니다.");
            return false;
        }
        
        if (template.getMessage() == null || template.getMessage().trim().isEmpty()) {
            log.warn("알림 메시지가 없습니다.");
            return false;
        }
        
        return true;
    }
    
    /**
     * 템플릿 변수 치환
     */
    public String replaceTemplateVariables(String template, Map<String, String> variables) {
        String result = template;
        
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        
        return result;
    }
    
    /**
     * 기본 변수 맵 생성
     */
    public Map<String, String> createDefaultVariables(User user) {
        Map<String, String> variables = new HashMap<>();
        variables.put("userName", user.getName());
        variables.put("userEmail", user.getEmail());
        variables.put("currentDate", LocalDateTime.now().toString());
        return variables;
    }
} 