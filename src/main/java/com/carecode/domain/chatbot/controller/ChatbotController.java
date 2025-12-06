package com.carecode.domain.chatbot.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.chatbot.dto.request.ChatbotRequestDto;
import com.carecode.domain.chatbot.dto.request.ChatbotMessageRequest;
import com.carecode.domain.chatbot.dto.response.ChatbotMessageResponse;
import com.carecode.domain.chatbot.dto.response.ChatbotChatHistoryDtoResponse;
import com.carecode.domain.chatbot.dto.response.ChatbotSessionDtoResponse;
import com.carecode.domain.chatbot.dto.response.ChatbotFeedbackDtoResponse;
import com.carecode.domain.chatbot.service.ChatbotService;
import com.carecode.domain.chatbot.app.ChatbotFacade;
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
    private final ChatbotFacade chatbotFacade;

    /**
     * 챗봇 메시지 전송
     */
    @PostMapping("/chat")
    @LogExecutionTime
    @Operation(summary = "챗봇 메시지 전송", description = "챗봇과 대화를 시작합니다.")
    public ResponseEntity<ChatbotMessageResponse> sendMessage(
            @Parameter(description = "챗봇 요청 정보", required = true) @RequestBody ChatbotMessageRequest request) {
        log.info("챗봇 메시지 전송: 사용자ID={}, 메시지={}", request.getUserId(), request.getMessage());
        
        try {
            ChatbotMessageResponse response = chatbotFacade.processMessage(request);
            return ResponseEntity.ok(response);
        } catch (CareServiceException e) {
            log.error("챗봇 처리 오류: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("챗봇 처리 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new CareServiceException("챗봇 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 대화 기록 조회
     */
    @GetMapping("/history")
    @LogExecutionTime
    @Operation(summary = "대화 기록 조회", description = "사용자의 챗봇 대화 기록을 조회합니다.")
    public ResponseEntity<List<ChatbotChatHistoryDtoResponse>> getChatHistory(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
            @Parameter(description = "세션 ID") @RequestParam(required = false) String sessionId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        log.info("대화 기록 조회: 사용자ID={}, 세션ID={}, 페이지={}, 크기={}", userId, sessionId, page, size);
        
        try {
            List<ChatbotChatHistoryDtoResponse> history = chatbotFacade.getChatHistory(userId, sessionId, page, size);
            return ResponseEntity.ok(history);
        } catch (CareServiceException e) {
            log.error("대화 기록 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 세션 목록 조회
     */
    @GetMapping("/sessions")
    @LogExecutionTime
    @Operation(summary = "세션 목록 조회", description = "사용자의 챗봇 세션 목록을 조회합니다.")
    public ResponseEntity<List<ChatbotSessionDtoResponse>> getSessions(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        log.info("세션 목록 조회: 사용자ID={}, 페이지={}, 크기={}", userId, page, size);
        
        try {
            List<ChatbotSessionDtoResponse> sessions = chatbotFacade.getSessions(userId, page, size);
            return ResponseEntity.ok(sessions);
        } catch (CareServiceException e) {
            log.error("세션 목록 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 메시지 피드백 처리
     */
    @PostMapping("/feedback")
    @LogExecutionTime
    @Operation(summary = "메시지 피드백 처리", description = "챗봇 메시지에 대한 피드백을 처리합니다.")
    public ResponseEntity<ChatbotFeedbackDtoResponse> processFeedback(
            @Parameter(description = "메시지 ID", required = true) @RequestParam Long messageId,
            @Parameter(description = "도움됨 여부", required = true) @RequestParam boolean isHelpful) {
        log.info("메시지 피드백 처리: 메시지ID={}, 도움됨={}", messageId, isHelpful);
        
        try {
            chatbotFacade.processFeedback(messageId, isHelpful);
            ChatbotFeedbackDtoResponse response = ChatbotFeedbackDtoResponse.builder()
                    .messageId(messageId)
                    .isHelpful(isHelpful)
                    .message("피드백이 성공적으로 처리되었습니다.")
                    .updatedAt(java.time.LocalDateTime.now())
                    .build();
            return ResponseEntity.ok(response);
        } catch (CareServiceException e) {
            log.error("메시지 피드백 처리 오류: {}", e.getMessage());
            throw e;
        }
    }

    // ==================== 챗봇 필터링 기능 ====================

    /**
     * 의도 타입별 메시지 조회
     */
    @GetMapping("/messages/intent")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "의도 타입별 메시지 조회", description = "특정 의도 타입의 메시지를 조회합니다.")
    public ResponseEntity<List<ChatbotChatHistoryDtoResponse>> getMessagesByIntentType(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
            @Parameter(description = "의도 타입 (GREETING, QUESTION, COMPLAINT, THANKS, GOODBYE, HEALTH_INFO, UNKNOWN)", required = true) @RequestParam String intentType) {
        List<ChatbotChatHistoryDtoResponse> messages = chatbotFacade.getMessagesByIntentType(
                userId, com.carecode.domain.chatbot.entity.ChatMessage.IntentType.valueOf(intentType));
        return ResponseEntity.ok(messages);
    }

    /**
     * 기간별 메시지 조회
     */
    @GetMapping("/messages/date-range")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "기간별 메시지 조회", description = "특정 기간의 메시지를 조회합니다.")
    public ResponseEntity<List<ChatbotChatHistoryDtoResponse>> getMessagesByDateRange(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
            @Parameter(description = "시작일시 (yyyy-MM-ddTHH:mm:ss)", required = true) @RequestParam String startDate,
            @Parameter(description = "종료일시 (yyyy-MM-ddTHH:mm:ss)", required = true) @RequestParam String endDate) {
        List<ChatbotChatHistoryDtoResponse> messages = chatbotFacade.getMessagesByDateRange(
                userId, java.time.LocalDateTime.parse(startDate), java.time.LocalDateTime.parse(endDate));
        return ResponseEntity.ok(messages);
    }

    /**
     * 도움됨 여부별 메시지 조회
     */
    @GetMapping("/messages/helpful")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "도움됨 여부별 메시지 조회", description = "도움됨/도움 안됨 여부별 메시지를 조회합니다.")
    public ResponseEntity<List<ChatbotChatHistoryDtoResponse>> getMessagesByHelpfulStatus(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
            @Parameter(description = "도움됨 여부", required = true) @RequestParam Boolean isHelpful) {
        List<ChatbotChatHistoryDtoResponse> messages = chatbotFacade.getMessagesByHelpfulStatus(userId, isHelpful);
        return ResponseEntity.ok(messages);
    }

    /**
     * 키워드로 메시지 검색
     */
    @GetMapping("/messages/search")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "키워드로 메시지 검색", description = "키워드로 메시지를 검색합니다.")
    public ResponseEntity<List<ChatbotChatHistoryDtoResponse>> searchMessagesByKeyword(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
            @Parameter(description = "검색 키워드", required = true) @RequestParam String keyword) {
        List<ChatbotChatHistoryDtoResponse> messages = chatbotFacade.searchMessagesByKeyword(userId, keyword);
        return ResponseEntity.ok(messages);
    }

    /**
     * 상태별 세션 조회
     */
    @GetMapping("/sessions/status")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "상태별 세션 조회", description = "특정 상태의 세션을 조회합니다.")
    public ResponseEntity<List<ChatbotSessionDtoResponse>> getSessionsByStatus(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
            @Parameter(description = "세션 상태 (ACTIVE, INACTIVE, CLOSED)", required = true) @RequestParam String status) {
        List<ChatbotSessionDtoResponse> sessions = chatbotFacade.getSessionsByStatus(
                userId, com.carecode.domain.chatbot.entity.ChatSession.SessionStatus.valueOf(status));
        return ResponseEntity.ok(sessions);
    }

    /**
     * 기간별 세션 조회
     */
    @GetMapping("/sessions/date-range")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "기간별 세션 조회", description = "특정 기간의 세션을 조회합니다.")
    public ResponseEntity<List<ChatbotSessionDtoResponse>> getSessionsByDateRange(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
            @Parameter(description = "시작일시 (yyyy-MM-ddTHH:mm:ss)", required = true) @RequestParam String startDate,
            @Parameter(description = "종료일시 (yyyy-MM-ddTHH:mm:ss)", required = true) @RequestParam String endDate) {
        List<ChatbotSessionDtoResponse> sessions = chatbotFacade.getSessionsByDateRange(
                userId, java.time.LocalDateTime.parse(startDate), java.time.LocalDateTime.parse(endDate));
        return ResponseEntity.ok(sessions);
    }

    /**
     * 사용자별 세션 수 조회
     */
    @GetMapping("/sessions/count")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자별 세션 수 조회", description = "사용자의 전체 세션 수를 조회합니다.")
    public ResponseEntity<Map<String, Long>> getSessionCountByUser(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId) {
        long count = chatbotFacade.getSessionCountByUser(userId);
        Map<String, Long> response = new java.util.HashMap<>();
        response.put("sessionCount", count);
        return ResponseEntity.ok(response);
    }
} 