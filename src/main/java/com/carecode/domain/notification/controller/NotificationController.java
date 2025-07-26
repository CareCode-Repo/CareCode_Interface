package com.carecode.domain.notification.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.notification.dto.NotificationRequestDto;
import com.carecode.domain.notification.dto.NotificationResponseDto;
import com.carecode.domain.notification.dto.NotificationPreferenceDto;
import com.carecode.domain.notification.service.NotificationService;
import com.carecode.domain.notification.service.NotificationPreferenceService;
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

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;

    /**
     * 알림 목록 조회
     */
    @GetMapping
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 목록 조회", description = "사용자의 알림 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = NotificationResponseDto.NotificationResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<NotificationResponseDto.NotificationResponse>> getAllNotifications(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {
        log.info("알림 목록 조회: 사용자ID={}", userId);
        
        try {
            List<NotificationResponseDto.NotificationResponse> notifications = notificationService.getNotificationsByUserId(userId);
            return ResponseEntity.ok(notifications);
        } catch (CareServiceException e) {
            log.error("알림 목록 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 알림 상세 조회
     */
    @GetMapping("/{notificationId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 상세 조회", description = "특정 알림의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = NotificationResponseDto.NotificationResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<NotificationResponseDto.NotificationResponse> getNotification(
            @Parameter(description = "알림 ID", required = true) @PathVariable Long notificationId) {
        log.info("알림 상세 조회: 알림ID={}", notificationId);
        
        try {
            NotificationResponseDto.NotificationResponse notification = notificationService.getNotificationById(notificationId);
            return ResponseEntity.ok(notification);
        } catch (CareServiceException e) {
            log.error("알림 상세 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 알림 생성
     */
    @PostMapping
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 생성", description = "새로운 알림을 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "생성 성공",
            content = @Content(schema = @Schema(implementation = NotificationResponseDto.NotificationResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<NotificationResponseDto.NotificationResponse> createNotification(
            @Parameter(description = "알림 정보", required = true) @RequestBody NotificationRequestDto.CreateNotificationRequest request) {
        log.info("알림 생성: 사용자ID={}, 제목={}", request.getUserId(), request.getTitle());
        
        try {
            NotificationResponseDto.NotificationResponse notification = notificationService.createNotification(request);
            return ResponseEntity.ok(notification);
        } catch (CareServiceException e) {
            log.error("알림 생성 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 알림 수정
     */
    @PutMapping("/{notificationId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 수정", description = "기존 알림을 수정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "수정 성공",
            content = @Content(schema = @Schema(implementation = NotificationResponseDto.NotificationResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<NotificationResponseDto.NotificationResponse> updateNotification(
            @Parameter(description = "알림 ID", required = true) @PathVariable Long notificationId,
            @Parameter(description = "수정할 알림 정보", required = true) @RequestBody NotificationRequestDto.CreateNotificationRequest request) {
        log.info("알림 수정: 알림ID={}", notificationId);
        
        try {
            NotificationResponseDto.NotificationResponse notification = notificationService.updateNotification(notificationId, request);
            return ResponseEntity.ok(notification);
        } catch (CareServiceException e) {
            log.error("알림 수정 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 알림 삭제
     */
    @DeleteMapping("/{notificationId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 삭제", description = "알림을 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, String>> deleteNotification(
            @Parameter(description = "알림 ID", required = true) @PathVariable Long notificationId) {
        log.info("알림 삭제: 알림ID={}", notificationId);
        
        try {
            notificationService.deleteNotification(notificationId);
            return ResponseEntity.ok(Map.of("message", "알림이 삭제되었습니다."));
        } catch (CareServiceException e) {
            log.error("알림 삭제 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 알림 읽음 처리
     */
    @PutMapping("/{notificationId}/read")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 읽음 처리", description = "알림을 읽음 상태로 변경합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "처리 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, String>> markAsRead(
            @Parameter(description = "알림 ID", required = true) @PathVariable Long notificationId) {
        log.info("알림 읽음 처리: 알림ID={}", notificationId);
        
        try {
            notificationService.markAsRead(notificationId);
            return ResponseEntity.ok(Map.of("message", "알림이 읽음 처리되었습니다."));
        } catch (CareServiceException e) {
            log.error("알림 읽음 처리 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 모든 알림 읽음 처리
     */
    @PutMapping("/read-all")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "모든 알림 읽음 처리", description = "사용자의 모든 알림을 읽음 상태로 변경합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "처리 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {
        log.info("모든 알림 읽음 처리: 사용자ID={}", userId);
        
        try {
            notificationService.markAllAsRead(userId);
            return ResponseEntity.ok(Map.of("message", "모든 알림이 읽음 처리되었습니다."));
        } catch (CareServiceException e) {
            log.error("모든 알림 읽음 처리 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 읽지 않은 알림 조회
     */
    @GetMapping("/unread")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "읽지 않은 알림 조회", description = "사용자의 읽지 않은 알림 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = NotificationResponseDto.NotificationResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<NotificationResponseDto.NotificationResponse>> getUnreadNotifications(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {
        log.info("읽지 않은 알림 조회: 사용자ID={}", userId);
        
        try {
            List<NotificationResponseDto.NotificationResponse> notifications = notificationService.getUnreadNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (CareServiceException e) {
            log.error("읽지 않은 알림 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 알림 설정 조회
     */
    @GetMapping("/settings/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 설정 조회", description = "사용자의 알림 설정을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getNotificationSettings(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        log.info("알림 설정 조회: 사용자ID={}", userId);
        
        try {
            Map<String, Object> settings = notificationService.getNotificationSettings(userId);
            return ResponseEntity.ok(settings);
        } catch (CareServiceException e) {
            log.error("알림 설정 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 알림 설정 업데이트
     */
    @PutMapping("/settings/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 설정 업데이트", description = "사용자의 알림 설정을 업데이트합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "업데이트 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> updateNotificationSettings(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
            @Parameter(description = "알림 설정", required = true) @RequestBody Map<String, Object> settings) {
        log.info("알림 설정 업데이트: 사용자ID={}", userId);
        
        try {
            Map<String, Object> updatedSettings = notificationService.updateNotificationSettings(userId, settings);
            return ResponseEntity.ok(updatedSettings);
        } catch (CareServiceException e) {
            log.error("알림 설정 업데이트 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 알림 통계 조회
     */
    @GetMapping("/statistics/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 통계 조회", description = "사용자의 알림 관련 통계를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getNotificationStatistics(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        log.info("알림 통계 조회: 사용자ID={}", userId);
        
        try {
            Map<String, Object> statistics = notificationService.getNotificationStatistics(userId);
            return ResponseEntity.ok(statistics);
        } catch (CareServiceException e) {
            log.error("알림 통계 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 알림 설정 목록 조회
     */
    @GetMapping("/preferences")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 설정 목록 조회", description = "사용자의 알림 설정 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<NotificationPreferenceDto>> getNotificationPreferences(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {
        log.info("알림 설정 목록 조회: 사용자ID={}", userId);
        
        try {
            List<NotificationPreferenceDto> preferences = preferenceService.getUserPreferences(userId);
            return ResponseEntity.ok(preferences);
        } catch (CareServiceException e) {
            log.error("알림 설정 목록 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 특정 알림 타입 설정 조회
     */
    @GetMapping("/preferences/{notificationType}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "특정 알림 타입 설정 조회", description = "사용자의 특정 알림 타입 설정을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<NotificationPreferenceDto> getNotificationPreferenceByType(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
            @Parameter(description = "알림 타입", required = true) @PathVariable String notificationType) {
        log.info("특정 알림 타입 설정 조회: 사용자ID={}, 타입={}", userId, notificationType);
        
        try {
            NotificationPreferenceDto preference = preferenceService.getPreferenceByType(userId, 
                com.carecode.domain.notification.entity.Notification.NotificationType.valueOf(notificationType));
            return ResponseEntity.ok(preference);
        } catch (CareServiceException e) {
            log.error("특정 알림 타입 설정 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 알림 설정 저장
     */
    @PostMapping("/preferences")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 설정 저장", description = "사용자의 알림 설정을 저장합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "저장 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<NotificationPreferenceDto> saveNotificationPreference(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
            @Parameter(description = "알림 설정", required = true) @RequestBody NotificationPreferenceDto preferenceDto) {
        log.info("알림 설정 저장: 사용자ID={}, 타입={}", userId, preferenceDto.getNotificationType());
        
        try {
            NotificationPreferenceDto savedPreference = preferenceService.savePreference(userId, preferenceDto);
            return ResponseEntity.ok(savedPreference);
        } catch (CareServiceException e) {
            log.error("알림 설정 저장 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 채널별 설정 업데이트
     */
    @PutMapping("/preferences/{notificationType}/channels/{channel}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "채널별 설정 업데이트", description = "사용자의 특정 알림 타입의 채널별 설정을 업데이트합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "업데이트 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<NotificationPreferenceDto> updateChannelPreference(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
            @Parameter(description = "알림 타입", required = true) @PathVariable String notificationType,
            @Parameter(description = "채널", required = true) @PathVariable String channel,
            @Parameter(description = "활성화 여부", required = true) @RequestParam boolean enabled) {
        log.info("채널별 설정 업데이트: 사용자ID={}, 타입={}, 채널={}, 활성화={}", userId, notificationType, channel, enabled);
        
        try {
            NotificationPreferenceDto updatedPreference = preferenceService.updateChannelPreference(userId, notificationType, channel, enabled);
            return ResponseEntity.ok(updatedPreference);
        } catch (CareServiceException e) {
            log.error("채널별 설정 업데이트 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 모든 알림 설정 비활성화
     */
    @PutMapping("/preferences/disable-all")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "모든 알림 설정 비활성화", description = "사용자의 모든 알림 설정을 비활성화합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "비활성화 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, String>> disableAllNotifications(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {
        log.info("모든 알림 설정 비활성화: 사용자ID={}", userId);
        
        try {
            preferenceService.disableAllNotifications(userId);
            return ResponseEntity.ok(Map.of("message", "모든 알림 설정이 비활성화되었습니다."));
        } catch (CareServiceException e) {
            log.error("모든 알림 설정 비활성화 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 알림 설정 기본값으로 초기화
     */
    @PutMapping("/preferences/reset")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "알림 설정 초기화", description = "사용자의 알림 설정을 기본값으로 초기화합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "초기화 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, String>> resetNotificationPreferences(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {
        log.info("알림 설정 초기화: 사용자ID={}", userId);
        
        try {
            preferenceService.resetToDefault(userId);
            return ResponseEntity.ok(Map.of("message", "알림 설정이 기본값으로 초기화되었습니다."));
        } catch (CareServiceException e) {
            log.error("알림 설정 초기화 오류: {}", e.getMessage());
            throw e;
        }
    }
} 