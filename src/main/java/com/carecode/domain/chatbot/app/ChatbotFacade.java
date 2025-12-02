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
}


