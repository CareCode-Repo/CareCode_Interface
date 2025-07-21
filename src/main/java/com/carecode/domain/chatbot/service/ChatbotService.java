package com.carecode.domain.chatbot.service;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.chatbot.dto.ChatbotRequestDto;
import com.carecode.domain.chatbot.dto.ChatbotResponseDto;
import com.carecode.domain.chatbot.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 육아 챗봇 서비스 UseCase
 * AI 기반 육아 상담 및 질문-답변 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    /**
     * 챗봇 메시지 처리
     */
    @LogExecutionTime
    public ChatbotResponseDto processMessage(ChatbotRequestDto.ChatbotRequest request) {
        log.info("챗봇 메시지 처리: 사용자ID={}, 메시지={}", request.getUserId(), request.getMessage());
        
        try {
            // 기본 응답 생성
            String response = generateBasicResponse(request.getMessage());
            
            return ChatbotResponseDto.builder()
                    .messageId(generateMessageId())
                    .sessionId(generateSessionId())
                    .sender("BOT")
                    .content(response)
                    .timestamp(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("챗봇 메시지 처리 실패: {}", e.getMessage());
            throw new CareServiceException("챗봇 메시지 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 대화 기록 조회
     */
    @LogExecutionTime
    public List<ChatMessageDto> getChatHistory(String userId) {
        log.info("대화 기록 조회: 사용자ID={}", userId);
        
        try {
            // 임시로 빈 리스트 반환 (실제로는 데이터베이스에서 조회)
            return List.of();
        } catch (Exception e) {
            log.error("대화 기록 조회 실패: {}", e.getMessage());
            throw new CareServiceException("대화 기록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 대화 기록 삭제
     */
    @LogExecutionTime
    public void deleteChatHistory(String userId) {
        log.info("대화 기록 삭제: 사용자ID={}", userId);
        
        try {
            // 실제로는 데이터베이스에서 삭제
            log.info("대화 기록이 삭제되었습니다: 사용자ID={}", userId);
        } catch (Exception e) {
            log.error("대화 기록 삭제 실패: {}", e.getMessage());
            throw new CareServiceException("대화 기록 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 메시지 조회
     */
    @LogExecutionTime
    public ChatMessageDto getMessageById(Long messageId) {
        log.info("메시지 조회: 메시지ID={}", messageId);
        
        try {
            // 임시 응답 (실제로는 데이터베이스에서 조회)
            return ChatMessageDto.builder()
                    .messageId(messageId.toString())
                    .sessionId("session_" + messageId)
                    .sender("BOT")
                    .content("임시 메시지 내용")
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("메시지 조회 실패: {}", e.getMessage());
            throw new CareServiceException("메시지 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 메시지 삭제
     */
    @LogExecutionTime
    public void deleteMessage(Long messageId) {
        log.info("메시지 삭제: 메시지ID={}", messageId);
        
        try {
            // 실제로는 데이터베이스에서 삭제
            log.info("메시지가 삭제되었습니다: 메시지ID={}", messageId);
        } catch (Exception e) {
            log.error("메시지 삭제 실패: {}", e.getMessage());
            throw new CareServiceException("메시지 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 챗봇 통계 조회
     */
    @LogExecutionTime
    public Map<String, Object> getChatbotStatistics() {
        log.info("챗봇 통계 조회");
        
        try {
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalSessions", 0);
            statistics.put("activeSessions", 0);
            statistics.put("totalMessages", 0);
            statistics.put("averageResponseTime", 0.5);
            statistics.put("userSatisfaction", 4.2);
            
            return statistics;
        } catch (Exception e) {
            log.error("챗봇 통계 조회 실패: {}", e.getMessage());
            throw new CareServiceException("챗봇 통계 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자별 챗봇 통계 조회
     */
    @LogExecutionTime
    public Map<String, Object> getUserChatbotStatistics(String userId) {
        log.info("사용자별 챗봇 통계 조회: 사용자ID={}", userId);
        
        try {
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("userId", userId);
            statistics.put("totalSessions", 0);
            statistics.put("totalMessages", 0);
            statistics.put("lastInteraction", LocalDateTime.now());
            statistics.put("preferredTopics", List.of("육아", "건강", "교육"));
            
            return statistics;
        } catch (Exception e) {
            log.error("사용자별 챗봇 통계 조회 실패: {}", e.getMessage());
            throw new CareServiceException("사용자별 챗봇 통계 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 챗봇 설정 조회
     */
    @LogExecutionTime
    public Map<String, Object> getChatbotSettings() {
        log.info("챗봇 설정 조회");
        
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("aiModel", "GPT-4");
            settings.put("responseLanguage", "ko");
            settings.put("maxResponseLength", 500);
            settings.put("enableContextMemory", true);
            settings.put("enablePersonalization", true);
            
            return settings;
        } catch (Exception e) {
            log.error("챗봇 설정 조회 실패: {}", e.getMessage());
            throw new CareServiceException("챗봇 설정 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 챗봇 설정 업데이트
     */
    @LogExecutionTime
    public Map<String, Object> updateChatbotSettings(Map<String, Object> settings) {
        log.info("챗봇 설정 업데이트");
        
        try {
            // 실제로는 설정을 저장
            return settings;
        } catch (Exception e) {
            log.error("챗봇 설정 업데이트 실패: {}", e.getMessage());
            throw new CareServiceException("챗봇 설정 업데이트 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 챗봇 상태 확인
     */
    @LogExecutionTime
    public boolean checkChatbotHealth() {
        log.info("챗봇 상태 확인");
        
        try {
            // 실제로는 다양한 서비스 상태를 확인
            return true;
        } catch (Exception e) {
            log.error("챗봇 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 기본 응답 생성
     */
    private String generateBasicResponse(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "안녕하세요! 육아에 대해 궁금한 점이 있으시면 언제든 물어보세요.";
        }
        
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("안녕") || lowerMessage.contains("hello")) {
            return "안녕하세요! 육아 도우미입니다. 무엇을 도와드릴까요?";
        } else if (lowerMessage.contains("먹이") || lowerMessage.contains("식사")) {
            return "아기의 식사는 연령에 따라 다릅니다. 구체적인 연령을 알려주시면 더 자세한 정보를 제공해드릴 수 있어요.";
        } else if (lowerMessage.contains("잠") || lowerMessage.contains("수면")) {
            return "아기의 수면 패턴은 성장 단계에 따라 달라집니다. 연령별 수면 가이드를 제공해드릴 수 있어요.";
        } else if (lowerMessage.contains("놀이") || lowerMessage.contains("놀기")) {
            return "아기와의 놀이는 발달에 매우 중요합니다. 연령에 맞는 놀이 방법을 추천해드릴 수 있어요.";
        } else {
            return "좋은 질문이네요! 육아에 대해 더 구체적으로 알려주시면 더 정확한 답변을 드릴 수 있어요.";
        }
    }

    /**
     * 메시지 ID 생성
     */
    private String generateMessageId() {
        return "msg_" + System.currentTimeMillis();
    }



    /**
     * 육아 질문에 대한 AI 응답 생성
     */
    @LogExecutionTime
    public ChatbotResponse generateResponse(String question, int childAge, String context) {
        log.info("챗봇 응답 생성: 질문={}, 자녀연령={}", question, childAge);
        
        try {
            // 1. 질문 분석 및 분류
            QuestionCategory category = analyzeQuestion(question);
            
            // 2. 연령별 맞춤 정보 검색
            List<String> ageSpecificInfo = searchAgeSpecificInfo(childAge, category);
            
            // 3. AI 모델을 통한 응답 생성
            String aiResponse = generateAIResponse(question, ageSpecificInfo, context);
            
            // 4. 관련 정보 및 링크 추가
            List<RelatedInfo> relatedInfo = findRelatedInfo(category, childAge);
            
            return ChatbotResponse.builder()
                    .response(aiResponse)
                    .category(category)
                    .confidence(calculateConfidence(question, aiResponse))
                    .relatedInfo(relatedInfo)
                    .suggestedQuestions(generateSuggestedQuestions(category, childAge))
                    .build();
                    
        } catch (Exception e) {
            log.error("챗봇 응답 생성 실패: {}", e.getMessage());
            throw new CareServiceException("CHATBOT_ERROR", "챗봇 응답 생성 중 오류가 발생했습니다.");
        }
    }

    /**
     * 육아 상담 세션 시작
     */
    @LogExecutionTime
    @RequireAuthentication
    public ChatSession startChatSession(String userId, int childAge, String childGender, 
                                      String parentType, String concerns) {
        log.info("챗봇 세션 시작: 사용자ID={}, 자녀연령={}, 관심사={}", userId, childAge, concerns);
        
        // 1. 사용자 프로필 생성
        UserProfile profile = createUserProfile(userId, childAge, childGender, parentType);
        
        // 2. 초기 상담 질문 생성
        List<String> initialQuestions = generateInitialQuestions(profile, concerns);
        
        // 3. 세션 정보 저장
        ChatSession session = ChatSession.builder()
                .sessionId(generateSessionId())
                .userId(userId)
                .profile(profile)
                .status("ACTIVE")
                .startTime(java.time.LocalDateTime.now())
                .initialQuestions(initialQuestions)
                .build();
        
        saveChatSession(session);
        
        return session;
    }

    /**
     * 대화 히스토리 기반 맞춤 응답
     */
    @LogExecutionTime
    @RequireAuthentication
    public ChatbotResponse generateContextualResponse(String sessionId, String question, 
                                                    List<ChatMessage> conversationHistory) {
        log.info("맥락 기반 응답 생성: 세션ID={}, 질문={}", sessionId, question);
        
        // 1. 대화 히스토리 분석
        ConversationContext context = analyzeConversationContext(conversationHistory);
        
        // 2. 사용자 패턴 파악
        UserPattern pattern = analyzeUserPattern(conversationHistory);
        
        // 3. 맥락을 고려한 응답 생성
        String contextualResponse = generateContextualAIResponse(question, context, pattern);
        
        // 4. 후속 질문 생성
        List<String> followUpQuestions = generateFollowUpQuestions(context, pattern);
        
        return ChatbotResponse.builder()
                .response(contextualResponse)
                .category(analyzeQuestion(question))
                .confidence(calculateContextualConfidence(question, context))
                .followUpQuestions(followUpQuestions)
                .build();
    }

    /**
     * 육아 지식 베이스 검색
     */
    @LogExecutionTime
    public List<KnowledgeBaseEntry> searchKnowledgeBase(String query, int childAge, 
                                                       String category, int limit) {
        log.info("지식 베이스 검색: 쿼리={}, 자녀연령={}, 카테고리={}", query, childAge, category);
        
        try {
            // 1. 키워드 추출
            List<String> keywords = extractKeywords(query);
            
            // 2. 연령 관련 지식 검색
            List<KnowledgeBaseEntry> ageRelevantEntries = searchAgeRelevantKnowledge(childAge, keywords);
            
            // 3. 카테고리 필터링
            List<KnowledgeBaseEntry> categoryFiltered = filterByCategory(ageRelevantEntries, category);
            
            // 4. 관련성 순으로 정렬
            List<KnowledgeBaseEntry> rankedEntries = rankByRelevance(categoryFiltered, query);
            
            // 5. 제한된 수만 반환
            return rankedEntries.subList(0, Math.min(limit, rankedEntries.size()));
            
        } catch (Exception e) {
            log.error("지식 베이스 검색 실패: {}", e.getMessage());
            throw new CareServiceException("지식 베이스 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 육아 상담 피드백 수집
     */
    @LogExecutionTime
    public void collectFeedback(String sessionId, String responseId, int rating, 
                              String feedback, String improvement) {
        log.info("피드백 수집: 세션ID={}, 응답ID={}, 평점={}", sessionId, responseId, rating);
        
        try {
            // 1. 피드백 데이터 저장
            FeedbackData feedbackData = FeedbackData.builder()
                    .sessionId(sessionId)
                    .responseId(responseId)
                    .rating(rating)
                    .feedback(feedback)
                    .improvement(improvement)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
            
            saveFeedback(feedbackData);
            
            // 2. 모델 훈련 데이터 업데이트
            updateModelTrainingData(responseId, rating, feedback);
            
        } catch (Exception e) {
            log.error("피드백 수집 실패: {}", e.getMessage());
            throw new CareServiceException("피드백 수집 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자 메시지 처리 (기존 메서드)
     */
    @LogExecutionTime
    public String processUserMessage(String userId, String message) {
        log.info("사용자 메시지 처리: 사용자ID={}, 메시지={}", userId, message);
        
        try {
            // 간단한 응답 생성
            return generateBasicResponse(message);
        } catch (Exception e) {
            log.error("사용자 메시지 처리 실패: {}", e.getMessage());
            return "죄송합니다. 메시지 처리 중 오류가 발생했습니다.";
        }
    }

    // 기존 private 메서드들...
    private QuestionCategory analyzeQuestion(String question) {
        // 질문 분석 로직
        return QuestionCategory.GENERAL;
    }
    
    private List<String> searchAgeSpecificInfo(int childAge, QuestionCategory category) {
        // 연령별 정보 검색 로직
        return List.of("연령별 정보");
    }
    
    private String generateAIResponse(String question, List<String> ageSpecificInfo, String context) {
        // AI 응답 생성 로직
        return "AI 응답";
    }
    
    private List<RelatedInfo> findRelatedInfo(QuestionCategory category, int childAge) {
        // 관련 정보 검색 로직
        return List.of();
    }
    
    private double calculateConfidence(String question, String response) {
        // 신뢰도 계산 로직
        return 0.8;
    }
    
    private List<String> generateSuggestedQuestions(QuestionCategory category, int childAge) {
        // 제안 질문 생성 로직
        return List.of("추천 질문");
    }
    
    private UserProfile createUserProfile(String userId, int childAge, String childGender, String parentType) {
        return UserProfile.builder()
                .userId(userId)
                .childAge(childAge)
                .childGender(childGender)
                .parentType(parentType)
                .build();
    }
    
    private List<String> generateInitialQuestions(UserProfile profile, String concerns) {
        // 초기 질문 생성 로직
        return List.of("초기 질문");
    }
    
    /**
     * 세션 ID 생성
     */
    private String generateSessionId() {
        return "session_" + System.currentTimeMillis();
    }
    
    private void saveChatSession(ChatSession session) {
        // 세션 저장 로직
    }
    
    private ConversationContext analyzeConversationContext(List<ChatMessage> conversationHistory) {
        // 대화 맥락 분석 로직
        return new ConversationContext();
    }
    
    private UserPattern analyzeUserPattern(List<ChatMessage> conversationHistory) {
        // 사용자 패턴 분석 로직
        return new UserPattern();
    }
    
    private String generateContextualAIResponse(String question, ConversationContext context, UserPattern pattern) {
        // 맥락 기반 AI 응답 생성 로직
        return "맥락 기반 응답";
    }
    
    private List<String> generateFollowUpQuestions(ConversationContext context, UserPattern pattern) {
        // 후속 질문 생성 로직
        return List.of("후속 질문");
    }
    
    private double calculateContextualConfidence(String question, ConversationContext context) {
        // 맥락 기반 신뢰도 계산 로직
        return 0.9;
    }
    
    private List<String> extractKeywords(String query) {
        // 키워드 추출 로직
        return List.of("키워드");
    }
    
    private List<KnowledgeBaseEntry> searchAgeRelevantKnowledge(int childAge, List<String> keywords) {
        // 연령 관련 지식 검색 로직
        return List.of();
    }
    
    private List<KnowledgeBaseEntry> filterByCategory(List<KnowledgeBaseEntry> entries, String category) {
        // 카테고리 필터링 로직
        return entries;
    }
    
    private List<KnowledgeBaseEntry> rankByRelevance(List<KnowledgeBaseEntry> entries, String query) {
        // 관련성 순 정렬 로직
        return entries;
    }
    
    private void saveFeedback(FeedbackData feedbackData) {
        // 피드백 저장 로직
    }
    
    private void updateModelTrainingData(String responseId, int rating, String feedback) {
        // 모델 훈련 데이터 업데이트 로직
    }

    public enum QuestionCategory {
        MEDICAL, CARE_FACILITY, POLICY, DEVELOPMENT, GENERAL
    }

    public static class ChatbotResponse {
        private String response;
        private QuestionCategory category;
        private double confidence;
        private List<RelatedInfo> relatedInfo;
        private List<String> suggestedQuestions;
        private List<String> followUpQuestions;

        // Getters
        public String getResponse() { return response; }
        public QuestionCategory getCategory() { return category; }
        public double getConfidence() { return confidence; }
        public List<RelatedInfo> getRelatedInfo() { return relatedInfo; }
        public List<String> getSuggestedQuestions() { return suggestedQuestions; }
        public List<String> getFollowUpQuestions() { return followUpQuestions; }

        // Builder
        public static ChatbotResponseBuilder builder() {
            return new ChatbotResponseBuilder();
        }

        public static class ChatbotResponseBuilder {
            private ChatbotResponse result = new ChatbotResponse();

            public ChatbotResponseBuilder response(String response) {
                result.response = response;
                return this;
            }

            public ChatbotResponseBuilder category(QuestionCategory category) {
                result.category = category;
                return this;
            }

            public ChatbotResponseBuilder confidence(double confidence) {
                result.confidence = confidence;
                return this;
            }

            public ChatbotResponseBuilder relatedInfo(List<RelatedInfo> relatedInfo) {
                result.relatedInfo = relatedInfo;
                return this;
            }

            public ChatbotResponseBuilder suggestedQuestions(List<String> suggestedQuestions) {
                result.suggestedQuestions = suggestedQuestions;
                return this;
            }

            public ChatbotResponseBuilder followUpQuestions(List<String> followUpQuestions) {
                result.followUpQuestions = followUpQuestions;
                return this;
            }

            public ChatbotResponse build() {
                return result;
            }
        }
    }

    public static class ChatSession {
        private String sessionId;
        private String userId;
        private UserProfile profile;
        private String status;
        private java.time.LocalDateTime startTime;
        private List<String> initialQuestions;

        // Getters
        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public UserProfile getProfile() { return profile; }
        public String getStatus() { return status; }
        public java.time.LocalDateTime getStartTime() { return startTime; }
        public List<String> getInitialQuestions() { return initialQuestions; }

        // Builder
        public static ChatSessionBuilder builder() {
            return new ChatSessionBuilder();
        }

        public static class ChatSessionBuilder {
            private ChatSession result = new ChatSession();

            public ChatSessionBuilder sessionId(String sessionId) {
                result.sessionId = sessionId;
                return this;
            }

            public ChatSessionBuilder userId(String userId) {
                result.userId = userId;
                return this;
            }

            public ChatSessionBuilder profile(UserProfile profile) {
                result.profile = profile;
                return this;
            }

            public ChatSessionBuilder status(String status) {
                result.status = status;
                return this;
            }

            public ChatSessionBuilder startTime(java.time.LocalDateTime startTime) {
                result.startTime = startTime;
                return this;
            }

            public ChatSessionBuilder initialQuestions(List<String> initialQuestions) {
                result.initialQuestions = initialQuestions;
                return this;
            }

            public ChatSession build() {
                return result;
            }
        }
    }

    public static class UserProfile {
        private String userId;
        private int childAge;
        private String childGender;
        private String parentType;

        // Getters
        public String getUserId() { return userId; }
        public int getChildAge() { return childAge; }
        public String getChildGender() { return childGender; }
        public String getParentType() { return parentType; }

        // Builder
        public static UserProfileBuilder builder() {
            return new UserProfileBuilder();
        }

        public static class UserProfileBuilder {
            private UserProfile result = new UserProfile();

            public UserProfileBuilder userId(String userId) {
                result.userId = userId;
                return this;
            }

            public UserProfileBuilder childAge(int childAge) {
                result.childAge = childAge;
                return this;
            }

            public UserProfileBuilder childGender(String childGender) {
                result.childGender = childGender;
                return this;
            }

            public UserProfileBuilder parentType(String parentType) {
                result.parentType = parentType;
                return this;
            }

            public UserProfile build() {
                return result;
            }
        }
    }

    public static class ChatMessage {
        private String messageId;
        private String sessionId;
        private String sender; // "USER" or "BOT"
        private String content;
        private java.time.LocalDateTime timestamp;

        // Getters and Setters
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public java.time.LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(java.time.LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class ConversationContext {
        private String mainTopic;
        private List<String> discussedTopics;
        private int conversationLength;
        private String userMood;
    }

    public static class UserPattern {
        private String preferredCategory;
        private String communicationStyle;
        private List<String> frequentConcerns;
    }

    public static class RelatedInfo {
        private String title;
        private String description;
        private String url;
        private String category;

        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }

    public static class KnowledgeBaseEntry {
        private String entryId;
        private String title;
        private String content;
        private String category;
        private int minAge;
        private int maxAge;
        private double relevanceScore;

        // Getters and Setters
        public String getEntryId() { return entryId; }
        public void setEntryId(String entryId) { this.entryId = entryId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public int getMinAge() { return minAge; }
        public void setMinAge(int minAge) { this.minAge = minAge; }
        public int getMaxAge() { return maxAge; }
        public void setMaxAge(int maxAge) { this.maxAge = maxAge; }
        public double getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }
    }

    public static class FeedbackData {
        private String sessionId;
        private String responseId;
        private int rating;
        private String feedback;
        private String improvement;
        private java.time.LocalDateTime timestamp;

        // Builder
        public static FeedbackDataBuilder builder() {
            return new FeedbackDataBuilder();
        }

        public static class FeedbackDataBuilder {
            private FeedbackData result = new FeedbackData();

            public FeedbackDataBuilder sessionId(String sessionId) {
                result.sessionId = sessionId;
                return this;
            }

            public FeedbackDataBuilder responseId(String responseId) {
                result.responseId = responseId;
                return this;
            }

            public FeedbackDataBuilder rating(int rating) {
                result.rating = rating;
                return this;
            }

            public FeedbackDataBuilder feedback(String feedback) {
                result.feedback = feedback;
                return this;
            }

            public FeedbackDataBuilder improvement(String improvement) {
                result.improvement = improvement;
                return this;
            }

            public FeedbackDataBuilder timestamp(java.time.LocalDateTime timestamp) {
                result.timestamp = timestamp;
                return this;
            }

            public FeedbackData build() {
                return result;
            }
        }
    }
} 