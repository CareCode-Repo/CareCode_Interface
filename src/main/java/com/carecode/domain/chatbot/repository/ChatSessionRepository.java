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

    // 세션 ID로 세션 조회
    Optional<ChatSession> findBySessionId(String sessionId);

    // 사용자별 세션 목록 조회 (페이징)
    Page<ChatSession> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // 사용자별 활성 세션 조회
    List<ChatSession> findByUserAndStatusOrderByCreatedAtDesc(User user, ChatSession.SessionStatus status);

    // 기간별 세션 조회
    @Query("SELECT cs FROM ChatSession cs WHERE cs.user = :user AND cs.createdAt BETWEEN :startDate AND :endDate ORDER BY cs.createdAt DESC")
    List<ChatSession> findByUserAndDateRange(@Param("user") User user, 
                                           @Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);

    // 사용자별 세션 수 조회
    long countByUser(User user);
}