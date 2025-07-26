package com.carecode.domain.chatbot.repository;

import com.carecode.domain.chatbot.entity.ChatMessage;
import com.carecode.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 챗봇 메시지 리포지토리
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 사용자별 메시지 목록 조회 (최신순)
     */
    List<ChatMessage> findByUserOrderByCreatedAtDesc(User user);

    /**
     * 사용자별 메시지 목록 조회 (페이징)
     */
    Page<ChatMessage> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 세션별 메시지 목록 조회
     */
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /**
     * 사용자별 최근 메시지 조회
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.user = :user ORDER BY cm.createdAt DESC")
    List<ChatMessage> findRecentMessagesByUser(@Param("user") User user, Pageable pageable);

    /**
     * 의도 타입별 메시지 조회
     */
    List<ChatMessage> findByUserAndIntentTypeOrderByCreatedAtDesc(User user, ChatMessage.IntentType intentType);

    /**
     * 기간별 메시지 조회
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.user = :user AND cm.createdAt BETWEEN :startDate AND :endDate ORDER BY cm.createdAt DESC")
    List<ChatMessage> findByUserAndDateRange(@Param("user") User user, 
                                           @Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * 도움됨 여부별 메시지 조회
     */
    List<ChatMessage> findByUserAndIsHelpfulOrderByCreatedAtDesc(User user, Boolean isHelpful);

    /**
     * 사용자별 메시지 수 조회
     */
    long countByUser(User user);

    /**
     * 세션별 메시지 수 조회
     */
    long countBySessionId(String sessionId);

    /**
     * 의도 타입별 메시지 수 조회
     */
    long countByUserAndIntentType(User user, ChatMessage.IntentType intentType);

    /**
     * 사용자별 최근 대화 세션 조회
     */
    @Query("SELECT DISTINCT cm.sessionId FROM ChatMessage cm WHERE cm.user = :user ORDER BY cm.createdAt DESC")
    List<String> findRecentSessionIdsByUser(@Param("user") User user, Pageable pageable);

    /**
     * 키워드 검색
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.user = :user AND (cm.message LIKE %:keyword% OR cm.response LIKE %:keyword%) ORDER BY cm.createdAt DESC")
    List<ChatMessage> findByUserAndKeyword(@Param("user") User user, @Param("keyword") String keyword);
} 