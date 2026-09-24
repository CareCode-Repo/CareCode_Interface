package com.carecode.core.ops.sync;

import com.carecode.core.ops.OperationalAlerter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.List;

/**
 * 데이터가 낡았는지 매일 확인한다.
 *
 * <p>동기화가 멈춰도 사용자 화면은 그대로여서, 알림이 없으면 누군가 "요즘 목록이 안 늘던데" 라고
 * 말할 때까지 모른다. 작업별 기준 시간을 넘기면 한 번 묶어서 알린다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncFreshnessScheduler {

    /**
     * 기동 직후에는 이력이 없어 전부 "낡음" 으로 보인다. 새 서버가 첫 주기를 돌 시간을 준다.
     * 가장 긴 일간 작업 기준(36시간)보다 넉넉하게 잡는다.
     */
    private static final Duration GRACE_AFTER_STARTUP = Duration.ofHours(48);

    private final SyncFreshnessService freshnessService;
    private final OperationalAlerter alerter;

    @Scheduled(cron = "${app.scheduler.freshness.cron:0 0 11 * * *}", zone = "Asia/Seoul")
    public void checkFreshness() {
        List<SyncFreshnessService.JobFreshness> stale = freshnessService.describeAll().stream()
                .filter(SyncFreshnessService.JobFreshness::isStale)
                .filter(job -> job.getLastFreshAt() != null || uptimeExceedsGrace())
                .toList();

        if (stale.isEmpty()) {
            log.debug("데이터 신선도 점검: 기준을 넘긴 작업 없음");
            return;
        }

        String detail = stale.stream()
                .map(job -> "- " + job.getLabel() + ": "
                        + (job.getLastFreshAt() == null
                                ? "성공 기록 없음"
                                : job.getAgeHours() + "시간 전 (기준 " + job.getStaleAfterHours() + "시간)")
                        + (job.getLastStatus() != null ? ", 마지막 실행 " + job.getLastStatus() : ""))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        log.warn("데이터 신선도 기준을 넘긴 작업 {}건\n{}", stale.size(), detail);
        alerter.alert("sync-stale", "데이터가 낡았습니다 (" + stale.size() + "건)", detail);
    }

    private static boolean uptimeExceedsGrace() {
        return ManagementFactory.getRuntimeMXBean().getUptime() > GRACE_AFTER_STARTUP.toMillis();
    }
}
