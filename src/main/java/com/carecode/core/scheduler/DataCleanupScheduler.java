package com.carecode.core.scheduler;

import com.carecode.domain.user.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 주기적인 데이터 정리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCleanupScheduler {

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    /**
     * 만료·사용 완료된 이메일 인증 토큰 정리. 매일 새벽 4시.
     * 정리하지 않으면 가입 시도마다 행이 쌓여 테이블이 무한히 커진다.
     */
    @Scheduled(cron = "${app.scheduler.cleanup.cron:0 0 4 * * *}", zone = "Asia/Seoul")
    @Transactional
    public void cleanupExpiredVerificationTokens() {
        // 만료 직후 바로 지우지 않고 하루 여유를 둬서, 사용자 문의 시 이력을 확인할 수 있게 한다.
        int deleted = emailVerificationTokenRepository.deleteExpiredOrUsed(LocalDateTime.now().minusDays(1));
        if (deleted > 0) {
            log.info("만료된 이메일 인증 토큰 정리 완료 - 건수={}", deleted);
        }
    }
}
