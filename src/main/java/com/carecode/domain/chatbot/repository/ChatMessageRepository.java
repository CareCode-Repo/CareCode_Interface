package com.carecode.domain.chatbot.repository;

import com.carecode.domain.chatbot.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 채팅 메시지 리포지토리 인터페이스
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * 세션별 메시지 목록 조회
     */
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);
    
    /**
     * 사용자별 메시지 목록 조회
     */
    List<ChatMessage> findByUserIdOrderByTimestampDesc(Long userId);
    
    /**
     * 읽지 않은 메시지 조회
     */
    List<ChatMessage> findByUserIdAndIsReadFalse(Long userId);
    
    /**
     * 세션별 메시지 개수 조회
     */
    long countBySessionId(String sessionId);
    
    /**
     * 사용자별 메시지 개수 조회
     */
    long countByUserId(Long userId);
} 