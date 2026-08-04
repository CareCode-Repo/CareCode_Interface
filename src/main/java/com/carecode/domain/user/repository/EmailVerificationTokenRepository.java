package com.carecode.domain.user.repository;

import com.carecode.domain.user.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);

    /**
     * 만료되었거나 이미 사용된 토큰을 정리한다.
     * 정리하지 않으면 테이블이 무한히 커진다.
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiryDate < :threshold OR t.used = true")
    int deleteExpiredOrUsed(@Param("threshold") LocalDateTime threshold);
} 