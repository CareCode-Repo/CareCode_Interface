package com.carecode.domain.chatbot.service;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.chatbot.dto.ChatbotRequestDto;
import com.carecode.domain.chatbot.dto.ChatbotResponseDto;
import com.carecode.domain.chatbot.entity.ChatMessage;
import com.carecode.domain.chatbot.entity.ChatSession;
import com.carecode.domain.chatbot.repository.ChatMessageRepository;
import com.carecode.domain.chatbot.repository.ChatSessionRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 챗봇 서비스 클래스
 * 육아 관련 챗봇 기능을 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatbotService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;

    // 의도 분석을 위한 키워드 패턴
    private static final Map<ChatMessage.IntentType, List<Pattern>> INTENT_PATTERNS = new HashMap<>();
    
    static {
        // 인사 패턴
        INTENT_PATTERNS.put(ChatMessage.IntentType.GREETING, Arrays.asList(
            Pattern.compile("안녕|하이|헬로|반가워|만나서"),
            Pattern.compile("안녕하세요|안녕하신가요|반갑습니다")
        ));
        
        // 질문 패턴
        INTENT_PATTERNS.put(ChatMessage.IntentType.QUESTION, Arrays.asList(
            Pattern.compile("무엇|뭐|어떻게|언제|어디서|왜|어떤"),
            Pattern.compile("\\?|\\?\\?|물어보고|궁금해|알려줘")
        ));
        
        // 불만/문의 패턴
        INTENT_PATTERNS.put(ChatMessage.IntentType.COMPLAINT, Arrays.asList(
            Pattern.compile("문제|불만|어려워|힘들어|도와줘|해결"),
            Pattern.compile("안되|안돼|오류|에러|버그")
        ));
        
        // 감사 패턴
        INTENT_PATTERNS.put(ChatMessage.IntentType.THANKS, Arrays.asList(
            Pattern.compile("감사|고마워|고맙습니다|감사합니다|thank"),
            Pattern.compile("도움|도움이|좋아|좋은")
        ));
        
        // 작별인사 패턴
        INTENT_PATTERNS.put(ChatMessage.IntentType.GOODBYE, Arrays.asList(
            Pattern.compile("안녕|잘가|바이|goodbye|bye"),
            Pattern.compile("다음에|나중에|끝|종료")
        ));
        
        // 건강 정보 패턴
        INTENT_PATTERNS.put(ChatMessage.IntentType.HEALTH_INFO, Arrays.asList(
            Pattern.compile("건강|병원|의사|약|증상|아프|열|기침"),
            Pattern.compile("예방접종|백신|검진|진찰|치료")
        ));
        
        // 정책 정보 패턴
        INTENT_PATTERNS.put(ChatMessage.IntentType.POLICY_INFO, Arrays.asList(
            Pattern.compile("정책|지원|보조금|혜택|혜택|도움"),
            Pattern.compile("신청|지원금|수당|급여|복지")
        ));
        
        // 시설 정보 패턴
        INTENT_PATTERNS.put(ChatMessage.IntentType.FACILITY_INFO, Arrays.asList(
            Pattern.compile("어린이집|유치원|보육|시설|원"),
            Pattern.compile("위치|주소|전화|연락처|운영시간")
        ));
        
        // 교육 정보 패턴
        INTENT_PATTERNS.put(ChatMessage.IntentType.EDUCATION_INFO, Arrays.asList(
            Pattern.compile("교육|학습|공부|프로그램|강의"),
            Pattern.compile("육아|양육|부모|아이|아동")
        ));
    }

    /**
     * 챗봇 메시지 처리
     */
    @LogExecutionTime
    @Transactional
    public ChatbotResponseDto processMessage(ChatbotRequestDto.ChatbotRequest request) {
        log.info("챗봇 메시지 처리: 사용자ID={}, 메시지={}", request.getUserId(), request.getMessage());
        
        try {
            // 사용자 조회
            User user = userRepository.findByUserId(request.getUserId())
                    .orElseThrow(() -> new CareServiceException("사용자를 찾을 수 없습니다."));
            
            // 세션 관리
            ChatSession session = getOrCreateSession(user, request.getSessionId());
            
            // 의도 분석
            ChatMessage.IntentType intentType = analyzeIntent(request.getMessage());
            double confidence = calculateConfidence(request.getMessage(), intentType);
            
            // 응답 생성
            String response = generateResponse(request.getMessage(), intentType, user);
            
            // 메시지 저장
            ChatMessage chatMessage = saveChatMessage(user, session, request.getMessage(), response, intentType, confidence);
            
            // 세션 업데이트
            updateSession(session, request.getMessage());
            
            return ChatbotResponseDto.builder()
                    .messageId(chatMessage.getId())
                    .response(response)
                    .intentType(intentType.name())
                    .confidence(confidence)
                    .sessionId(session.getSessionId())
                    .timestamp(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("챗봇 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new CareServiceException("챗봇 메시지 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 대화 기록 조회
     */
    @LogExecutionTime
    public List<ChatbotResponseDto.ChatHistoryResponse> getChatHistory(String userId, String sessionId, int page, int size) {
        log.info("대화 기록 조회: 사용자ID={}, 세션ID={}", userId, sessionId);
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new CareServiceException("사용자를 찾을 수 없습니다."));
            
            Pageable pageable = PageRequest.of(page, size);
            Page<ChatMessage> messages;
            
            if (sessionId != null && !sessionId.isEmpty()) {
                messages = chatMessageRepository.findByUserOrderByCreatedAtDesc(user, pageable);
            } else {
                messages = chatMessageRepository.findByUserOrderByCreatedAtDesc(user, pageable);
            }
            
            return messages.getContent().stream()
                    .map(this::convertToHistoryResponse)
                    .toList();
                    
        } catch (Exception e) {
            log.error("대화 기록 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new CareServiceException("대화 기록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 세션 목록 조회
     */
    @LogExecutionTime
    public List<ChatbotResponseDto.SessionResponse> getSessions(String userId, int page, int size) {
        log.info("세션 목록 조회: 사용자ID={}", userId);
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new CareServiceException("사용자를 찾을 수 없습니다."));
            
            Pageable pageable = PageRequest.of(page, size);
            Page<ChatSession> sessions = chatSessionRepository.findByUserOrderByCreatedAtDesc(user, pageable);
            
            return sessions.getContent().stream()
                    .map(this::convertToSessionResponse)
                    .toList();
                    
        } catch (Exception e) {
            log.error("세션 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new CareServiceException("세션 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 메시지 피드백 처리
     */
    @LogExecutionTime
    @Transactional
    public void processFeedback(Long messageId, boolean isHelpful) {
        log.info("메시지 피드백 처리: 메시지ID={}, 도움됨={}", messageId, isHelpful);
        
        try {
            ChatMessage message = chatMessageRepository.findById(messageId)
                    .orElseThrow(() -> new CareServiceException("메시지를 찾을 수 없습니다."));
            
            message.setIsHelpful(isHelpful);
            chatMessageRepository.save(message);
            
        } catch (Exception e) {
            log.error("메시지 피드백 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new CareServiceException("메시지 피드백 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 세션 생성 또는 조회
     */
    private ChatSession getOrCreateSession(User user, String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            return chatSessionRepository.findBySessionId(sessionId)
                    .orElseGet(() -> createNewSession(user, sessionId));
        } else {
            return createNewSession(user, generateSessionId());
        }
    }

    /**
     * 새 세션 생성
     */
    private ChatSession createNewSession(User user, String sessionId) {
        ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .user(user)
                .title("새로운 대화")
                .status(ChatSession.SessionStatus.ACTIVE)
                .messageCount(0)
                .lastActivityAt(LocalDateTime.now())
                .build();
        
        return chatSessionRepository.save(session);
    }

    /**
     * 세션 ID 생성
     */
    private String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 의도 분석
     */
    private ChatMessage.IntentType analyzeIntent(String message) {
        String lowerMessage = message.toLowerCase();
        
        for (Map.Entry<ChatMessage.IntentType, List<Pattern>> entry : INTENT_PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(lowerMessage).find()) {
                    return entry.getKey();
                }
            }
        }
        
        return ChatMessage.IntentType.UNKNOWN;
    }

    /**
     * 신뢰도 계산
     */
    private double calculateConfidence(String message, ChatMessage.IntentType intentType) {
        if (intentType == ChatMessage.IntentType.UNKNOWN) {
            return 0.1;
        }
        
        String lowerMessage = message.toLowerCase();
        int matchCount = 0;
        
        List<Pattern> patterns = INTENT_PATTERNS.get(intentType);
        for (Pattern pattern : patterns) {
            if (pattern.matcher(lowerMessage).find()) {
                matchCount++;
            }
        }
        
        return Math.min(0.9, 0.3 + (matchCount * 0.2));
    }

    /**
     * 응답 생성
     */
    private String generateResponse(String message, ChatMessage.IntentType intentType, User user) {
        switch (intentType) {
            case GREETING:
                return generateGreetingResponse(user);
            case QUESTION:
                return generateQuestionResponse(message);
            case COMPLAINT:
                return generateComplaintResponse();
            case THANKS:
                return generateThanksResponse();
            case GOODBYE:
                return generateGoodbyeResponse();
            case HEALTH_INFO:
                return generateHealthInfoResponse(message);
            case POLICY_INFO:
                return generatePolicyInfoResponse(message);
            case FACILITY_INFO:
                return generateFacilityInfoResponse(message);
            case EDUCATION_INFO:
                return generateEducationInfoResponse(message);
            default:
                return generateDefaultResponse();
        }
    }

    /**
     * 인사 응답 생성
     */
    private String generateGreetingResponse(User user) {
        return String.format("안녕하세요, %s님! 육아에 관한 궁금한 점이 있으시면 언제든 물어보세요. 건강, 정책, 시설, 교육 등 다양한 정보를 제공해드릴 수 있습니다.", user.getName());
    }

    /**
     * 질문 응답 생성
     */
    private String generateQuestionResponse(String message) {
        return "좋은 질문이네요! 구체적으로 어떤 부분에 대해 알고 싶으신지 말씀해 주시면 더 자세히 답변해드릴 수 있습니다.";
    }

    /**
     * 불만/문의 응답 생성
     */
    private String generateComplaintResponse() {
        return "불편하신 점이 있으시군요. 구체적인 상황을 말씀해 주시면 해결 방법을 찾아보겠습니다. 필요하시면 고객센터로 연결해드릴 수도 있습니다.";
    }

    /**
     * 감사 응답 생성
     */
    private String generateThanksResponse() {
        return "도움이 되었다니 기쁩니다! 앞으로도 육아에 관한 궁금한 점이 있으시면 언제든 찾아주세요.";
    }

    /**
     * 작별인사 응답 생성
     */
    private String generateGoodbyeResponse() {
        return "안녕히 가세요! 언제든 다시 찾아주세요. 육아에 관한 궁금한 점이 생기시면 언제든 도움을 드릴 준비가 되어 있습니다.";
    }

    /**
     * 건강 정보 응답 생성
     */
    private String generateHealthInfoResponse(String message) {
        if (message.contains("예방접종") || message.contains("백신")) {
            return "예방접종은 아이의 건강을 지키는 중요한 방법입니다. 연령별 예방접종 일정과 주의사항을 확인해보세요. 구체적인 질문이 있으시면 더 자세히 답변해드릴 수 있습니다.";
        } else if (message.contains("병원") || message.contains("의사")) {
            return "아이가 아프거나 건강상 문제가 있을 때는 소아과 전문의와 상담하는 것이 좋습니다. 주변 소아과 병원 정보를 찾아보시겠어요?";
        } else {
            return "아이의 건강에 관한 궁금한 점이 있으시군요. 예방접종, 건강검진, 질병 관리 등 다양한 건강 정보를 제공해드릴 수 있습니다. 구체적으로 어떤 부분에 대해 알고 싶으신가요?";
        }
    }

    /**
     * 정책 정보 응답 생성
     */
    private String generatePolicyInfoResponse(String message) {
        if (message.contains("보조금") || message.contains("지원금")) {
            return "육아 지원금과 보조금에 관한 정보를 제공해드릴 수 있습니다. 연령, 소득, 지역에 따라 지원 내용이 다를 수 있으니 구체적인 상황을 알려주시면 더 정확한 정보를 제공해드릴 수 있습니다.";
        } else if (message.contains("신청") || message.contains("혜택")) {
            return "다양한 육아 지원 정책과 혜택이 있습니다. 어린이집 지원, 양육수당, 교육비 지원 등이 있으니 구체적으로 어떤 혜택에 대해 알고 싶으신지 말씀해 주세요.";
        } else {
            return "육아 관련 정책과 지원 제도에 관한 정보를 제공해드릴 수 있습니다. 보조금, 지원금, 혜택 등 어떤 부분에 대해 알고 싶으신가요?";
        }
    }

    /**
     * 시설 정보 응답 생성
     */
    private String generateFacilityInfoResponse(String message) {
        if (message.contains("어린이집") || message.contains("유치원")) {
            return "어린이집과 유치원 정보를 제공해드릴 수 있습니다. 위치, 운영시간, 정원, 특별활동 등 구체적으로 어떤 정보가 필요하신가요?";
        } else if (message.contains("위치") || message.contains("주소")) {
            return "주변 육아 시설의 위치와 주소 정보를 찾아드릴 수 있습니다. 어느 지역의 시설을 찾고 계신가요?";
        } else {
            return "육아 관련 시설 정보를 제공해드릴 수 있습니다. 어린이집, 유치원, 놀이터, 도서관 등 어떤 시설에 대해 알고 싶으신가요?";
        }
    }

    /**
     * 교육 정보 응답 생성
     */
    private String generateEducationInfoResponse(String message) {
        if (message.contains("육아") || message.contains("양육")) {
            return "육아와 양육에 관한 다양한 교육 프로그램과 정보를 제공해드릴 수 있습니다. 부모 교육, 양육 스킬, 발달 단계별 놀이 등 어떤 부분에 관심이 있으신가요?";
        } else if (message.contains("프로그램") || message.contains("강의")) {
            return "다양한 육아 교육 프로그램과 강의 정보를 제공해드릴 수 있습니다. 온라인 강의, 오프라인 프로그램, 워크샵 등 어떤 형태의 교육을 찾고 계신가요?";
        } else {
            return "육아 교육에 관한 정보를 제공해드릴 수 있습니다. 부모 교육, 아이 발달, 놀이 방법 등 어떤 부분에 대해 알고 싶으신가요?";
        }
    }

    /**
     * 기본 응답 생성
     */
    private String generateDefaultResponse() {
        return "죄송합니다. 질문을 정확히 이해하지 못했습니다. 육아에 관한 건강, 정책, 시설, 교육 등 어떤 부분에 대해 궁금하신지 다시 말씀해 주세요.";
    }

    /**
     * 메시지 저장
     */
    private ChatMessage saveChatMessage(User user, ChatSession session, String message, String response, 
                                      ChatMessage.IntentType intentType, double confidence) {
        ChatMessage chatMessage = ChatMessage.builder()
                .user(user)
                .message(message)
                .response(response)
                .messageType(ChatMessage.MessageType.USER)
                .intentType(intentType)
                .confidence(confidence)
                .sessionId(session.getSessionId())
                .build();
        
        return chatMessageRepository.save(chatMessage);
    }

    /**
     * 세션 업데이트
     */
    private void updateSession(ChatSession session, String message) {
        session.setMessageCount(session.getMessageCount() + 1);
        session.setLastActivityAt(LocalDateTime.now());
        
        // 첫 번째 메시지인 경우 제목 설정
        if (session.getMessageCount() == 1) {
            String title = message.length() > 20 ? message.substring(0, 20) + "..." : message;
            session.setTitle(title);
        }
        
        chatSessionRepository.save(session);
    }

    /**
     * 대화 기록 응답 변환
     */
    private ChatbotResponseDto.ChatHistoryResponse convertToHistoryResponse(ChatMessage message) {
        return ChatbotResponseDto.ChatHistoryResponse.builder()
                .messageId(message.getId())
                .message(message.getMessage())
                .response(message.getResponse())
                .messageType(message.getMessageType().name())
                .intentType(message.getIntentType().name())
                .confidence(message.getConfidence())
                .sessionId(message.getSessionId())
                .isHelpful(message.getIsHelpful())
                .createdAt(message.getCreatedAt())
                .build();
    }

    /**
     * 세션 응답 변환
     */
    private ChatbotResponseDto.SessionResponse convertToSessionResponse(ChatSession session) {
        return ChatbotResponseDto.SessionResponse.builder()
                .sessionId(session.getSessionId())
                .title(session.getTitle())
                .description(session.getDescription())
                .status(session.getStatus().name())
                .messageCount(session.getMessageCount())
                .lastActivityAt(session.getLastActivityAt())
                .createdAt(session.getCreatedAt())
                .build();
    }
} 