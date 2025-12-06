package com.carecode.domain.chatbot.app;

import com.carecode.domain.chatbot.dto.request.ChatbotRequestDto;
import com.carecode.domain.chatbot.dto.request.ChatbotMessageRequest;
import com.carecode.domain.chatbot.dto.response.ChatbotMessageResponse;
import com.carecode.domain.chatbot.dto.response.ChatbotChatHistoryDtoResponse;
import com.carecode.domain.chatbot.dto.response.ChatbotSessionDtoResponse;
import com.carecode.domain.chatbot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatbotFacade {

    private final ChatbotService chatbotService;

    @Transactional
    public ChatbotMessageResponse processMessage(ChatbotMessageRequest request) {
        return chatbotService.processMessage(request);
    }

    @Transactional(readOnly = true)
    public List<ChatbotChatHistoryDtoResponse> getChatHistory(String userId, String sessionId, int page, int size) {
        return chatbotService.getChatHistory(userId, sessionId, page, size);
    }

    @Transactional(readOnly = true)
    public List<ChatbotSessionDtoResponse> getSessions(String userId, int page, int size) {
        return chatbotService.getSessions(userId, page, size);
    }

    @Transactional
    public void processFeedback(Long messageId, boolean isHelpful) {
        chatbotService.processFeedback(messageId, isHelpful);
    }

    @Transactional(readOnly = true)
    public List<ChatbotChatHistoryDtoResponse> getMessagesByIntentType(String userId, com.carecode.domain.chatbot.entity.ChatMessage.IntentType intentType) {
        return chatbotService.getMessagesByIntentType(userId, intentType);
    }

    @Transactional(readOnly = true)
    public List<ChatbotChatHistoryDtoResponse> getMessagesByDateRange(String userId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        return chatbotService.getMessagesByDateRange(userId, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<ChatbotChatHistoryDtoResponse> getMessagesByHelpfulStatus(String userId, Boolean isHelpful) {
        return chatbotService.getMessagesByHelpfulStatus(userId, isHelpful);
    }

    @Transactional(readOnly = true)
    public List<ChatbotChatHistoryDtoResponse> searchMessagesByKeyword(String userId, String keyword) {
        return chatbotService.searchMessagesByKeyword(userId, keyword);
    }

    @Transactional(readOnly = true)
    public List<ChatbotSessionDtoResponse> getSessionsByStatus(String userId, com.carecode.domain.chatbot.entity.ChatSession.SessionStatus status) {
        return chatbotService.getSessionsByStatus(userId, status);
    }

    @Transactional(readOnly = true)
    public List<ChatbotSessionDtoResponse> getSessionsByDateRange(String userId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        return chatbotService.getSessionsByDateRange(userId, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public long getSessionCountByUser(String userId) {
        return chatbotService.getSessionCountByUser(userId);
    }
}


