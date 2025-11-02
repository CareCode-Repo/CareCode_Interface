package com.carecode.domain.chatbot.app;

import com.carecode.domain.chatbot.dto.ChatbotRequestDto;
import com.carecode.domain.chatbot.dto.ChatbotResponseDto;
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
    public ChatbotResponseDto processMessage(ChatbotRequestDto.ChatbotRequest request) {
        return chatbotService.processMessage(request);
    }

    @Transactional(readOnly = true)
    public List<ChatbotResponseDto.ChatHistoryResponse> getChatHistory(String userId, String sessionId, int page, int size) {
        return chatbotService.getChatHistory(userId, sessionId, page, size);
    }

    @Transactional(readOnly = true)
    public List<ChatbotResponseDto.SessionResponse> getSessions(String userId, int page, int size) {
        return chatbotService.getSessions(userId, page, size);
    }

    @Transactional
    public void processFeedback(Long messageId, boolean isHelpful) {
        chatbotService.processFeedback(messageId, isHelpful);
    }
}


