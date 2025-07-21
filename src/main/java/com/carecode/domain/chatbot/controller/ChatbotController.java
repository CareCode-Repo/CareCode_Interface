package com.carecode.domain.chatbot.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.chatbot.dto.ChatbotRequestDto;
import com.carecode.domain.chatbot.dto.ChatbotResponseDto;
import com.carecode.domain.chatbot.dto.ChatMessageDto;
import com.carecode.domain.chatbot.service.ChatbotService;
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
 * 챗봇 API 컨트롤러
 * 육아 관련 챗봇 서비스
 */
@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "챗봇", description = "육아 관련 챗봇 서비스 API")
public class ChatbotController extends BaseController {

    private final ChatbotService chatbotService;

    /**
     * 챗봇 메시지 전송
     */
    @PostMapping("/chat")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "챗봇 메시지 전송", description = "챗봇과 대화를 시작합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "응답 성공",
            content = @Content(schema = @Schema(implementation = ChatbotResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ChatbotResponseDto> sendMessage(
            @Parameter(description = "챗봇 요청 정보", required = true) @RequestBody ChatbotRequestDto.ChatbotRequest request) {
        log.info("챗봇 메시지 전송: 사용자ID={}, 메시지={}", request.getUserId(), request.getMessage());
        
        try {
            ChatbotResponseDto response = chatbotService.processMessage(request);
            return ResponseEntity.ok(response);
        } catch (CareServiceException e) {
            log.error("챗봇 처리 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 대화 기록 조회
     */
    @GetMapping("/history/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "대화 기록 조회", description = "사용자의 챗봇 대화 기록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ChatMessageDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        log.info("대화 기록 조회: 사용자ID={}", userId);
        
        try {
            List<ChatMessageDto> history = chatbotService.getChatHistory(userId);
            return ResponseEntity.ok(history);
        } catch (CareServiceException e) {
            log.error("대화 기록 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 대화 기록 삭제
     */
    @DeleteMapping("/history/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "대화 기록 삭제", description = "사용자의 챗봇 대화 기록을 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, String>> deleteChatHistory(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        log.info("대화 기록 삭제: 사용자ID={}", userId);
        
        try {
            chatbotService.deleteChatHistory(userId);
            return ResponseEntity.ok(Map.of("message", "대화 기록이 삭제되었습니다."));
        } catch (CareServiceException e) {
            log.error("대화 기록 삭제 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 특정 메시지 조회
     */
    @GetMapping("/messages/{messageId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "특정 메시지 조회", description = "특정 챗봇 메시지를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ChatMessageDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "메시지를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ChatMessageDto> getMessage(
            @Parameter(description = "메시지 ID", required = true) @PathVariable Long messageId) {
        log.info("특정 메시지 조회: 메시지ID={}", messageId);
        
        try {
            ChatMessageDto message = chatbotService.getMessageById(messageId);
            return ResponseEntity.ok(message);
        } catch (CareServiceException e) {
            log.error("메시지 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 메시지 삭제
     */
    @DeleteMapping("/messages/{messageId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "메시지 삭제", description = "특정 챗봇 메시지를 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "메시지를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, String>> deleteMessage(
            @Parameter(description = "메시지 ID", required = true) @PathVariable Long messageId) {
        log.info("메시지 삭제: 메시지ID={}", messageId);
        
        try {
            chatbotService.deleteMessage(messageId);
            return ResponseEntity.ok(Map.of("message", "메시지가 삭제되었습니다."));
        } catch (CareServiceException e) {
            log.error("메시지 삭제 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 챗봇 통계 조회
     */
    @GetMapping("/statistics")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "챗봇 통계 조회", description = "챗봇 사용 통계를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getChatbotStatistics() {
        log.info("챗봇 통계 조회");
        
        try {
            Map<String, Object> statistics = chatbotService.getChatbotStatistics();
            return ResponseEntity.ok(statistics);
        } catch (CareServiceException e) {
            log.error("챗봇 통계 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 사용자별 챗봇 통계 조회
     */
    @GetMapping("/statistics/{userId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자별 챗봇 통계 조회", description = "특정 사용자의 챗봇 사용 통계를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getUserChatbotStatistics(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        log.info("사용자별 챗봇 통계 조회: 사용자ID={}", userId);
        
        try {
            Map<String, Object> statistics = chatbotService.getUserChatbotStatistics(userId);
            return ResponseEntity.ok(statistics);
        } catch (CareServiceException e) {
            log.error("사용자별 챗봇 통계 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 챗봇 설정 조회
     */
    @GetMapping("/settings")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "챗봇 설정 조회", description = "챗봇 설정 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getChatbotSettings() {
        log.info("챗봇 설정 조회");
        
        try {
            Map<String, Object> settings = chatbotService.getChatbotSettings();
            return ResponseEntity.ok(settings);
        } catch (CareServiceException e) {
            log.error("챗봇 설정 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 챗봇 설정 업데이트
     */
    @PutMapping("/settings")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "챗봇 설정 업데이트", description = "챗봇 설정을 업데이트합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "업데이트 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> updateChatbotSettings(
            @Parameter(description = "챗봇 설정", required = true) @RequestBody Map<String, Object> settings) {
        log.info("챗봇 설정 업데이트");
        
        try {
            Map<String, Object> updatedSettings = chatbotService.updateChatbotSettings(settings);
            return ResponseEntity.ok(updatedSettings);
        } catch (CareServiceException e) {
            log.error("챗봇 설정 업데이트 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 챗봇 상태 확인
     */
    @GetMapping("/health")
    @LogExecutionTime
    @Operation(summary = "챗봇 상태 확인", description = "챗봇 서비스의 상태를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 상태"),
        @ApiResponse(responseCode = "503", description = "서비스 불가능"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, String>> checkChatbotHealth() {
        log.info("챗봇 상태 확인");
        
        try {
            boolean isHealthy = chatbotService.checkChatbotHealth();
            if (isHealthy) {
                return ResponseEntity.ok(Map.of("status", "healthy", "message", "챗봇 서비스가 정상입니다."));
            } else {
                return ResponseEntity.status(503).body(Map.of("status", "unhealthy", "message", "챗봇 서비스에 문제가 있습니다."));
            }
        } catch (CareServiceException e) {
            log.error("챗봇 상태 확인 오류: {}", e.getMessage());
            throw e;
        }
    }
} 