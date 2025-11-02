package com.carecode.domain.chatbot.repository;

import com.carecode.domain.chatbot.entity.ChatSession;
import com.carecode.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 챗봇 세션 리포지토리
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    /**
     * 세션 ID로 세션 조회
     */
    Optional<ChatSession> findBySessionId(String sessionId);

    /**
     * 사용자별 세션 목록 조회 (최신순)
     */
    List<ChatSession> findByUserOrderByCreatedAtDesc(User user);

    /**
     * 사용자별 세션 목록 조회 (페이징)
     */
    Page<ChatSession> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 사용자별 활성 세션 조회
     */
    List<ChatSession> findByUserAndStatusOrderByCreatedAtDesc(User user, ChatSession.SessionStatus status);

    /**
     * 사용자별 최근 세션 조회
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.user = :user ORDER BY cs.lastActivityAt DESC")
    List<ChatSession> findRecentSessionsByUser(@Param("user") User user, Pageable pageable);

    /**
     * 기간별 세션 조회
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.user = :user AND cs.createdAt BETWEEN :startDate AND :endDate ORDER BY cs.createdAt DESC")
    List<ChatSession> findByUserAndDateRange(@Param("user") User user, 
                                           @Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자별 세션 수 조회
     */
    long countByUser(User user);

    /**
     * 상태별 세션 수 조회
     */
    long countByUserAndStatus(User user, ChatSession.SessionStatus status);

    /**
     * 오래된 세션 조회 (정리용)
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.lastActivityAt < :cutoffDate AND cs.status = 'ACTIVE'")
    List<ChatSession> findOldActiveSessions(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 제목으로 세션 검색
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.user = :user AND cs.title LIKE %:keyword% ORDER BY cs.createdAt DESC")
    List<ChatSession> findByUserAndTitleContaining(@Param("user") User user, @Param("keyword") String keyword);

    /**
     * 메시지 수가 많은 세션 조회
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.user = :user ORDER BY cs.messageCount DESC")
    List<ChatSession> findByUserOrderByMessageCountDesc(@Param("user") User user, Pageable pageable);
} 