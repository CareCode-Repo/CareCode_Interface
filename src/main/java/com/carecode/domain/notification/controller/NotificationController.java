package com.carecode.domain.notification.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.notification.dto.request.NotificationRequest;
import com.carecode.domain.notification.dto.request.NotificationCreateRequest;
import com.carecode.domain.notification.dto.request.NotificationMarkAsReadRequest;
import com.carecode.domain.notification.dto.request.NotificationRegisterPushTokenRequest;
import com.carecode.domain.notification.dto.request.NotificationUpdateSettingsRequest;
import com.carecode.domain.notification.dto.request.NotificationSendTestRequest;
import com.carecode.domain.notification.dto.response.NotificationResponse;
import com.carecode.domain.notification.dto.response.NotificationInfoResponse;
import com.carecode.domain.notification.dto.response.NotificationSettingsResponse;
import com.carecode.domain.notification.dto.response.NotificationStatsResponse;
import com.carecode.domain.notification.dto.response.NotificationTemplateResponse;
import com.carecode.domain.notification.dto.response.NotificationDeliveryStatusResponse;
import com.carecode.domain.notification.app.NotificationFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.carecode.core.handler.ApiSuccess;
import java.util.Date;
import java.util.Map;

/**
 * 알림 API 컨트롤러
 * 육아 관련 알림 관리 서비스
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "알림 관리", description = "육아 관련 알림 관리 API")
public class NotificationController extends BaseController {

    private final NotificationFacade notificationFacade;

    /**
     * 알림 목록 조회
     */
    @GetMapping
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 목록 조회", description = "사용자의 알림 목록을 조회합니다.")
    public ResponseEntity<List<NotificationInfoResponse>> getAllNotifications(@Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {
        List<NotificationInfoResponse> notifications = notificationFacade.getNotificationsByUserId(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 알림 상세 조회
     */
    @GetMapping("/{notificationId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 상세 조회", description = "특정 알림의 상세 정보를 조회합니다.")
    public ResponseEntity<NotificationInfoResponse> getNotification(@Parameter(description = "알림 ID", required = true) @PathVariable Long notificationId) {

        NotificationInfoResponse notification = notificationFacade.getNotificationById(notificationId);

        return ResponseEntity.ok(notification);
    }

    /**
     * 알림 생성
     */
    @PostMapping
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 생성", description = "새로운 알림을 생성합니다.")
    public ResponseEntity<NotificationInfoResponse> createNotification(@Parameter(description = "알림 정보", required = true) @RequestBody NotificationCreateRequest request) {

        NotificationInfoResponse notification = notificationFacade.createNotification(request);

        return ResponseEntity.ok(notification);
    }

    /**
     * 알림 수정
     */
    @PutMapping("/{notificationId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 수정", description = "기존 알림을 수정합니다.")
    public ResponseEntity<NotificationInfoResponse> updateNotification(@Parameter(description = "알림 ID", required = true) @PathVariable Long notificationId,
                                                                                @Parameter(description = "수정할 알림 정보", required = true) @RequestBody NotificationCreateRequest request) {

        NotificationInfoResponse notification = notificationFacade.updateNotification(notificationId, request);

        return ResponseEntity.ok(notification);
    }

    /**
     * 알림 삭제
     */
    @DeleteMapping("/{notificationId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 삭제", description = "알림을 삭제합니다.")
    public ResponseEntity<ApiSuccess> deleteNotification(@Parameter(description = "알림 ID", required = true) @PathVariable Long notificationId) {

        notificationFacade.deleteNotification(notificationId);

        return ResponseEntity.ok(ApiSuccess.builder().timestamp(new Date()).message("알림이 삭제되었습니다.").build());
    }

    /**
     * 알림 읽음 처리
     */
    @PutMapping("/{notificationId}/read")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 읽음 처리", description = "알림을 읽음 상태로 변경합니다.")
    public ResponseEntity<ApiSuccess> markAsRead(@Parameter(description = "알림 ID", required = true) @PathVariable Long notificationId) {

        notificationFacade.markAsRead(notificationId);

        return ResponseEntity.ok(ApiSuccess.builder().timestamp(new Date()).message("알림이 읽음 처리되었습니다.").build());
    }

    /**
     * 모든 알림 읽음 처리
     */
    @PutMapping("/read-all")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "모든 알림 읽음 처리", description = "사용자의 모든 알림을 읽음 상태로 변경합니다.")
    public ResponseEntity<ApiSuccess> markAllAsRead(@Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {

        notificationFacade.markAllAsRead(userId);

        return ResponseEntity.ok(ApiSuccess.builder().timestamp(new Date()).message("모든 알림이 읽음 처리되었습니다.").build());
    }

    /**
     * 읽지 않은 알림 조회
     */
    @GetMapping("/unread")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "읽지 않은 알림 조회", description = "사용자의 읽지 않은 알림 목록을 조회합니다.")
    public ResponseEntity<List<NotificationInfoResponse>> getUnreadNotifications(@Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {

        List<NotificationInfoResponse> notifications = notificationFacade.getUnreadNotifications(userId);

        return ResponseEntity.ok(notifications);
    }

    /**
     * 알림 설정 조회
     */
    @GetMapping("/settings/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 설정 조회", description = "사용자의 알림 설정을 조회합니다.")
    public ResponseEntity<Map<String, Object>> getNotificationSettings(@Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {

        Map<String, Object> settings = notificationFacade.getNotificationSettings(userId);

        return ResponseEntity.ok(settings);
    }

    /**
     * 알림 설정 업데이트
     */
    @PutMapping("/settings/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 설정 업데이트", description = "사용자의 알림 설정을 업데이트합니다.")
    public ResponseEntity<Map<String, Object>> updateNotificationSettings(@Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
                                                                          @Parameter(description = "알림 설정", required = true) @RequestBody Map<String, Object> settings) {

        Map<String, Object> updatedSettings = notificationFacade.updateNotificationSettings(userId, settings);

        return ResponseEntity.ok(updatedSettings);
    }

    /**
     * 알림 통계 조회
     */
    @GetMapping("/statistics/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 통계 조회", description = "사용자의 알림 관련 통계를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getNotificationStatistics(@Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {

        Map<String, Object> statistics = notificationFacade.getNotificationStatistics(userId);

        return ResponseEntity.ok(statistics);
    }

    /**
     * 알림 설정 목록 조회
     */
    @GetMapping("/preferences")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 설정 목록 조회", description = "사용자의 알림 설정 목록을 조회합니다.")
    public ResponseEntity<List<NotificationSettingsResponse>> getNotificationPreferences(@Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {

        List<NotificationSettingsResponse> preferences = notificationFacade.getUserPreferences(userId);

        return ResponseEntity.ok(preferences);
    }

    /**
     * 특정 알림 타입 설정 조회
     */
    @GetMapping("/preferences/{notificationType}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "특정 알림 타입 설정 조회", description = "사용자의 특정 알림 타입 설정을 조회합니다.")
    public ResponseEntity<NotificationSettingsResponse> getNotificationPreferenceByType(@Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
                                                                                         @Parameter(description = "알림 타입", required = true) @PathVariable String notificationType) {
        NotificationSettingsResponse preference = notificationFacade.getPreferenceByType(userId, notificationType);

        return ResponseEntity.ok(preference);
    }

    /**
     * 전체 알림 설정 업데이트
     */
    @PutMapping("/preferences")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "전체 알림 설정 업데이트", description = "사용자의 모든 알림 설정을 한 번에 업데이트합니다.")
    public ResponseEntity<NotificationSettingsResponse> updateNotificationPreferences(@Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
                                                                                       @Parameter(description = "알림 설정", required = true) @RequestBody NotificationSettingsResponse preferenceDto) {

        NotificationSettingsResponse updatedPreference = notificationFacade.savePreference(userId, preferenceDto);

        return ResponseEntity.ok(updatedPreference);
    }

    /**
     * 알림 설정 저장
     */
    @PostMapping("/preferences")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 설정 저장", description = "사용자의 알림 설정을 저장합니다.")
    public ResponseEntity<NotificationSettingsResponse> saveNotificationPreference(@Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
                                                                                    @Parameter(description = "알림 설정", required = true) @RequestBody NotificationSettingsResponse preferenceDto) {

        NotificationSettingsResponse savedPreference = notificationFacade.savePreference(userId, preferenceDto);

        return ResponseEntity.ok(savedPreference);
    }

    /**
     * 채널별 설정 업데이트
     */
    @PutMapping("/preferences/{notificationType}/channels/{channel}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "채널별 설정 업데이트", description = "사용자의 특정 알림 타입의 채널별 설정을 업데이트합니다.")
    public ResponseEntity<NotificationSettingsResponse> updateChannelPreference(@Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
                                                                                 @Parameter(description = "알림 타입", required = true) @PathVariable String notificationType,
                                                                                 @Parameter(description = "채널", required = true) @PathVariable String channel,
                                                                                 @Parameter(description = "활성화 여부", required = true) @RequestParam boolean enabled) {

        NotificationSettingsResponse updatedPreference = notificationFacade.updateChannelPreference(userId, notificationType, channel, enabled);

        return ResponseEntity.ok(updatedPreference);
    }

    /**
     * 모든 알림 설정 비활성화
     */
    @PutMapping("/preferences/disable-all")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "모든 알림 설정 비활성화", description = "사용자의 모든 알림 설정을 비활성화합니다.")
    public ResponseEntity<ApiSuccess> disableAllNotifications(@Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {

        notificationFacade.disableAllNotifications(userId);

        return ResponseEntity.ok(ApiSuccess.builder().timestamp(new Date()).message("모든 알림 설정이 비활성화되었습니다.").build());
    }

    /**
     * 알림 설정 기본값으로 초기화
     */
    @PutMapping("/preferences/reset")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 설정 초기화", description = "사용자의 알림 설정을 기본값으로 초기화합니다.")
    public ResponseEntity<ApiSuccess> resetNotificationPreferences(@Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {

        notificationFacade.resetToDefault(userId);

        return ResponseEntity.ok(ApiSuccess.builder().timestamp(new Date()).message("알림 설정이 기본값으로 초기화되었습니다.").build());
    }

    /**
     * 알림 읽음 처리
     */
    @PutMapping("/mark-read")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 읽음 처리", description = "알림을 읽음으로 표시합니다.")
    public ResponseEntity<ApiSuccess> markAsRead(@Parameter(description = "읽음 처리 요청", required = true) @RequestBody NotificationMarkAsReadRequest request) {

        notificationFacade.markAsRead(request);

        return ResponseEntity.ok(ApiSuccess.builder().timestamp(new Date()).message("알림이 읽음으로 처리되었습니다.").build());
    }

    /**
     * 푸시 알림 토큰 등록
     */
    @PostMapping("/push-token")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "푸시 알림 토큰 등록", description = "사용자의 푸시 알림 토큰을 등록합니다.")
    public ResponseEntity<ApiSuccess> registerPushToken(@Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
                                                        @Parameter(description = "푸시 토큰 등록 요청", required = true) @RequestBody NotificationRegisterPushTokenRequest request) {

        notificationFacade.registerPushToken(userId, request);

        return ResponseEntity.ok(ApiSuccess.builder().timestamp(new Date()).message("푸시 알림 토큰이 등록되었습니다.").build());
    }

    /**
     * 알림 설정 수정
     */
    @PutMapping("/settings")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 설정 수정", description = "사용자의 알림 설정을 수정합니다.")
    public ResponseEntity<ApiSuccess> updateSettings(@Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
                                                     @Parameter(description = "설정 수정 요청", required = true) @RequestBody NotificationUpdateSettingsRequest request) {

        notificationFacade.updateSettings(userId, request);

        return ResponseEntity.ok(ApiSuccess.builder().timestamp(new Date()).message("알림 설정이 수정되었습니다.").build());
    }

    /**
     * 테스트 알림 발송
     */
    @PostMapping("/test")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "테스트 알림 발송", description = "테스트 알림을 발송합니다.")
    public ResponseEntity<ApiSuccess> sendTestNotification(@Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
                                                           @Parameter(description = "테스트 알림 요청", required = true) @RequestBody NotificationSendTestRequest request) {

        notificationFacade.sendTestNotification(userId, request);

        return ResponseEntity.ok(ApiSuccess.builder().timestamp(new Date()).message("테스트 알림이 발송되었습니다.").build());
    }

    /**
     * 알림 통계 조회
     */
    @GetMapping("/stats")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 통계 조회", description = "사용자의 알림 통계를 조회합니다.")
    public ResponseEntity<NotificationStatsResponse> getNotificationStats(@Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {

        NotificationStatsResponse stats = notificationFacade.getNotificationStats(userId);

        return ResponseEntity.ok(stats);
    }

    /**
     * 알림 템플릿 조회
     */
    @GetMapping("/templates")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 템플릿 조회", description = "알림 템플릿 목록을 조회합니다.")
    public ResponseEntity<List<NotificationTemplateResponse>> getNotificationTemplates(@Parameter(description = "알림 타입", required = false) @RequestParam(required = false) String type) {

        List<NotificationTemplateResponse> templates = notificationFacade.getNotificationTemplates(type);

        return ResponseEntity.ok(templates);
    }

    /**
     * 알림 전송 상태 조회
     */
    @GetMapping("/delivery-status/{notificationId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 전송 상태 조회", description = "특정 알림의 전송 상태를 조회합니다.")
    public ResponseEntity<NotificationDeliveryStatusResponse> getDeliveryStatus(@Parameter(description = "알림 ID", required = true) @PathVariable Long notificationId) {

        NotificationDeliveryStatusResponse status = notificationFacade.getDeliveryStatus(notificationId);

        return ResponseEntity.ok(status);
    }
} 