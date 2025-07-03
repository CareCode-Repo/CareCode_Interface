package com.carecode.domain.chatbot.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.chatbot.dto.ChatbotRequestDto;
import com.carecode.domain.chatbot.dto.ChatbotResponseDto;
import com.carecode.domain.chatbot.dto.ChatMessageDto;
import com.carecode.domain.chatbot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 육아 챗봇 API 컨트롤러
 * AI 기반 육아 상담 및 질문-답변 서비스
 */
@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController extends BaseController {

    private final ChatbotService chatbotService;

    /**
     * 육아 질문에 대한 AI 응답 생성
     */
    @PostMapping("/ask")
    @LogExecutionTime
    public ResponseEntity<ChatbotResponseDto.ChatbotResponse> askQuestion(@RequestBody ChatbotRequestDto.AskQuestionRequest request) {
        log.info("챗봇 질문 요청: {}", request.getQuestion());
        
        try {
            ChatbotService.ChatbotResponse response = chatbotService.generateResponse(
                request.getQuestion(), 
                request.getChildAge(), 
                request.getContext()
            );
            
            return ResponseEntity.ok(ChatbotResponseDto.ChatbotResponse.builder()
                .response(response.getResponse())
                .category(response.getCategory().toString())
                .confidence(response.getConfidence())
                .relatedInfo(response.getRelatedInfo().stream()
                    .map(info -> ChatbotResponseDto.RelatedInfoDto.builder()
                        .title(info.getTitle())
                        .description(info.getDescription())
                        .url(info.getUrl())
                        .category(info.getCategory())
                        .build())
                    .collect(java.util.stream.Collectors.toList()))
                .suggestedQuestions(response.getSuggestedQuestions())
                .followUpQuestions(response.getFollowUpQuestions())
                .build());
        } catch (CareServiceException e) {
            log.error("챗봇 서비스 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 육아 상담 세션 시작
     */
    @PostMapping("/session/start")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<ChatbotResponseDto.ChatSessionResponse> startChatSession(@RequestBody ChatbotRequestDto.StartSessionRequest request) {
        log.info("챗봇 세션 시작 요청: 사용자ID={}", request.getUserId());
        
        try {
            ChatbotService.ChatSession session = chatbotService.startChatSession(
                request.getUserId(),
                request.getChildAge(),
                request.getChildGender(),
                request.getParentType(),
                request.getConcerns()
            );
            
            return ResponseEntity.ok(ChatbotResponseDto.ChatSessionResponse.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .profile(ChatbotResponseDto.UserProfileDto.builder()
                    .userId(session.getProfile().getUserId())
                    .childAge(session.getProfile().getChildAge())
                    .childGender(session.getProfile().getChildGender())
                    .parentType(session.getProfile().getParentType())
                    .build())
                .status(session.getStatus())
                .startTime(session.getStartTime().toString())
                .initialQuestions(session.getInitialQuestions())
                .build());
        } catch (CareServiceException e) {
            log.error("챗봇 세션 시작 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 대화 히스토리 기반 맞춤 응답
     */
    @PostMapping("/session/{sessionId}/ask")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<ChatbotResponseDto.ChatbotResponse> askContextualQuestion(
            @PathVariable String sessionId,
            @RequestBody ChatbotRequestDto.ContextualQuestionRequest request) {
        log.info("맥락 기반 질문 요청: 세션ID={}", sessionId);
        
        try {
            // ChatMessageDto를 ChatMessage로 변환
            List<ChatbotService.ChatMessage> conversationHistory = request.getConversationHistory().stream()
                .map(dto -> {
                    ChatbotService.ChatMessage message = new ChatbotService.ChatMessage();
                    message.setMessageId(dto.getMessageId());
                    message.setSessionId(dto.getSessionId());
                    message.setSender(dto.getSender());
                    message.setContent(dto.getContent());
                    if (dto.getTimestamp() != null) {
                        message.setTimestamp(java.time.LocalDateTime.parse(dto.getTimestamp()));
                    }
                    return message;
                })
                .collect(java.util.stream.Collectors.toList());
            
            ChatbotService.ChatbotResponse response = chatbotService.generateContextualResponse(
                sessionId,
                request.getQuestion(),
                conversationHistory
            );
            
            return ResponseEntity.ok(ChatbotResponseDto.ChatbotResponse.builder()
                .response(response.getResponse())
                .category(response.getCategory().toString())
                .confidence(response.getConfidence())
                .relatedInfo(response.getRelatedInfo().stream()
                    .map(info -> ChatbotResponseDto.RelatedInfoDto.builder()
                        .title(info.getTitle())
                        .description(info.getDescription())
                        .url(info.getUrl())
                        .category(info.getCategory())
                        .build())
                    .collect(java.util.stream.Collectors.toList()))
                .suggestedQuestions(response.getSuggestedQuestions())
                .followUpQuestions(response.getFollowUpQuestions())
                .build());
        } catch (CareServiceException e) {
            log.error("맥락 기반 응답 생성 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 육아 지식 베이스 검색
     */
    @GetMapping("/knowledge")
    @LogExecutionTime
    public ResponseEntity<List<ChatbotResponseDto.KnowledgeBaseResponse>> searchKnowledge(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int childAge,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("지식 베이스 검색: 쿼리={}, 자녀연령={}", query, childAge);
        
        try {
            List<ChatbotService.KnowledgeBaseEntry> entries = chatbotService.searchKnowledgeBase(
                query, childAge, category, limit
            );
            
            List<ChatbotResponseDto.KnowledgeBaseResponse> responses = entries.stream()
                .map(entry -> ChatbotResponseDto.KnowledgeBaseResponse.builder()
                    .entryId(entry.getEntryId())
                    .title(entry.getTitle())
                    .content(entry.getContent())
                    .category(entry.getCategory())
                    .minAge(entry.getMinAge())
                    .maxAge(entry.getMaxAge())
                    .relevanceScore(entry.getRelevanceScore())
                    .build())
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(responses);
        } catch (CareServiceException e) {
            log.error("지식 베이스 검색 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 육아 상담 피드백 수집
     */
    @PostMapping("/feedback")
    @LogExecutionTime
    public ResponseEntity<Map<String, String>> submitFeedback(@RequestBody ChatbotRequestDto.FeedbackRequest request) {
        log.info("피드백 제출: 세션ID={}, 응답ID={}, 평점={}", 
                request.getSessionId(), request.getResponseId(), request.getRating());
        
        try {
            chatbotService.collectFeedback(
                request.getSessionId(),
                request.getResponseId(),
                request.getRating(),
                request.getFeedback(),
                request.getImprovement()
            );
            
            return ResponseEntity.ok(Map.of("message", SUCCESS_MESSAGE));
        } catch (CareServiceException e) {
            log.error("피드백 제출 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 챗봇 세션 종료
     */
    @PostMapping("/session/{sessionId}/end")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<Map<String, String>> endChatSession(@PathVariable String sessionId) {
        log.info("챗봇 세션 종료: 세션ID={}", sessionId);
        
        // 세션 종료 로직 구현
        return ResponseEntity.ok(Map.of("message", SUCCESS_MESSAGE));
    }

    /**
     * 챗봇 통계 조회
     */
    @GetMapping("/stats")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<ChatbotResponseDto.ChatbotStatsResponse> getChatbotStats(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String period) {
        log.info("챗봇 통계 조회: 사용자ID={}, 기간={}", userId, period);
        
        // 통계 조회 로직 구현
        ChatbotResponseDto.ChatbotStatsResponse stats = ChatbotResponseDto.ChatbotStatsResponse.builder()
            .totalSessions(0)
            .activeSessions(0)
            .averageRating(0.0)
            .categoryDistribution(Map.of())
            .ageGroupDistribution(Map.of())
            .build();
        return ResponseEntity.ok(stats);
    }
} 