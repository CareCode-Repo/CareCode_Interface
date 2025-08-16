package com.carecode.domain.chatbot.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.chatbot.dto.ChatbotRequestDto;
import com.carecode.domain.chatbot.dto.ChatbotResponseDto;
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<ChatbotResponseDto.ChatHistoryResponse>> getChatHistory(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
            @Parameter(description = "세션 ID") @RequestParam(required = false) String sessionId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        log.info("대화 기록 조회: 사용자ID={}, 세션ID={}, 페이지={}, 크기={}", userId, sessionId, page, size);
        
        try {
            List<ChatbotResponseDto.ChatHistoryResponse> history = chatbotService.getChatHistory(userId, sessionId, page, size);
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<ChatbotResponseDto.SessionResponse>> getSessions(
            @Parameter(description = "사용자 ID", required = true) @RequestParam String userId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        log.info("세션 목록 조회: 사용자ID={}, 페이지={}, 크기={}", userId, page, size);
        
        try {
            List<ChatbotResponseDto.SessionResponse> sessions = chatbotService.getSessions(userId, page, size);
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "처리 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ChatbotResponseDto.FeedbackResponse> processFeedback(
            @Parameter(description = "메시지 ID", required = true) @RequestParam Long messageId,
            @Parameter(description = "도움됨 여부", required = true) @RequestParam boolean isHelpful) {
        log.info("메시지 피드백 처리: 메시지ID={}, 도움됨={}", messageId, isHelpful);
        
        try {
            chatbotService.processFeedback(messageId, isHelpful);
            ChatbotResponseDto.FeedbackResponse response = ChatbotResponseDto.FeedbackResponse.builder()
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


} 