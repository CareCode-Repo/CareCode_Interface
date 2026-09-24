package com.carecode.core.ops.sync;

import com.carecode.core.client.sync.SyncResult;
import com.carecode.core.ops.OperationalAlerter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * 주기 작업을 감싸 실행 이력을 남긴다.
 *
 * <p>예전에는 결과를 로그로만 남겨서 "마지막으로 성공한 게 언제인가" 를 질의할 수 없었고,
 * 작업이 예외로 죽으면 스케줄러 스레드에서 스택만 찍히고 지나갔다. 여기서 잡아 이력에 남기고
 * 운영 알림을 보낸 뒤 삼킨다 — 한 작업의 실패가 다음 작업을 막지 않아야 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncRunTracker {

    private static final int DETAIL_MAX_LENGTH = 1000;

    private final SyncRunRepository syncRunRepository;
    private final OperationalAlerter alerter;

    /** 공공데이터 동기화처럼 {@link SyncResult} 를 돌려주는 작업. */
    public void track(SyncJob job, Supplier<SyncResult> action) {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            SyncResult result = action.get();
            SyncRun.Status status = statusOf(result);
            record(job, status, startedAt,
                    result.getCreated() + result.getUpdated(), result.getFailed(), result.toString());
            report(job, status, result.toString());
        } catch (RuntimeException e) {
            failed(job, startedAt, e);
        }
    }

    /** 알림 작업처럼 돌려주는 값이 없는 작업. */
    public void track(SyncJob job, Runnable action) {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            action.run();
            record(job, SyncRun.Status.SUCCESS, startedAt, 0, 0, null);
        } catch (RuntimeException e) {
            failed(job, startedAt, e);
        }
    }

    /**
     * 이미 실행한 동기화 결과를 이력에 남긴다. 관리자 수동 실행용.
     *
     * <p>수동 실행은 호출한 사람이 응답으로 결과를 바로 보므로 예외를 여기서 삼키지 않는다.
     * (그래서 예외로 끝난 수동 실행은 이력에 남지 않는다 — 조용히 지나가지 않으니 문제되지 않는다.)
     */
    public void recordSyncResult(SyncJob job, LocalDateTime startedAt, SyncResult result) {
        record(job, statusOf(result), startedAt,
                result.getCreated() + result.getUpdated(), result.getFailed(), result.toString());
    }

    /** 결과 형태가 SyncResult 가 아닌 작업(좌표 보정 등)의 성공 기록. */
    public void recordSuccess(SyncJob job, LocalDateTime startedAt, int processed, int failed, String detail) {
        record(job, failed > 0 ? SyncRun.Status.PARTIAL : SyncRun.Status.SUCCESS, startedAt, processed, failed, detail);
    }

    private void failed(SyncJob job, LocalDateTime startedAt, RuntimeException e) {
        log.error("{} 작업이 예외로 중단됐습니다", job.getLabel(), e);
        record(job, SyncRun.Status.FAILED, startedAt, 0, 0, e.getClass().getSimpleName() + ": " + e.getMessage());
        alerter.alert("sync-exception-" + job.getCode(), job.getLabel() + " 작업 실패", String.valueOf(e.getMessage()));
    }

    private static SyncRun.Status statusOf(SyncResult result) {
        if (!result.isCompleted()) {
            return SyncRun.Status.INCOMPLETE;
        }
        return result.getFailed() > 0 ? SyncRun.Status.PARTIAL : SyncRun.Status.SUCCESS;
    }

    private void report(SyncJob job, SyncRun.Status status, String detail) {
        if (status == SyncRun.Status.INCOMPLETE) {
            log.warn("{} 동기화 미완료 - {}", job.getLabel(), detail);
            alerter.alert("sync-" + job.getCode(), job.getLabel() + " 동기화 미완료", detail);
        } else if (status == SyncRun.Status.PARTIAL) {
            alerter.alert("sync-failed-" + job.getCode(), job.getLabel() + " 동기화 중 일부 실패", detail);
        } else {
            log.info("{} 동기화 완료 - {}", job.getLabel(), detail);
        }
    }

    /**
     * 이력 저장은 별도 트랜잭션이다. 작업이 자기 트랜잭션을 롤백해도 "돌았고 실패했다" 는 사실은 남아야 한다.
     * 이력 저장 자체가 실패해도 작업 결과를 덮어써서는 안 되므로 여기서 삼킨다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(SyncJob job, SyncRun.Status status, LocalDateTime startedAt,
                       int processed, int failed, String detail) {
        LocalDateTime finishedAt = LocalDateTime.now();
        try {
            syncRunRepository.save(SyncRun.builder()
                    .job(job.getCode())
                    .status(status)
                    .startedAt(startedAt)
                    .finishedAt(finishedAt)
                    .durationMillis(Duration.between(startedAt, finishedAt).toMillis())
                    .processed(processed)
                    .failed(failed)
                    .detail(truncate(detail))
                    .build());
        } catch (RuntimeException e) {
            log.error("작업 이력 저장 실패 - job={} status={}", job.getCode(), status, e);
        }
    }

    private static String truncate(String detail) {
        if (detail == null) {
            return null;
        }
        return detail.length() <= DETAIL_MAX_LENGTH ? detail : detail.substring(0, DETAIL_MAX_LENGTH);
    }
}
