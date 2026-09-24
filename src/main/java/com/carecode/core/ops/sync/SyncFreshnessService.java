package com.carecode.core.ops.sync;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 주기 작업이 얼마나 오래 성공하지 못했는지 본다.
 *
 * <p>동기화가 멈춰도 화면은 예전 데이터를 그대로 보여준다. 사람이 눈치채기 전에 알려면
 * "마지막 성공 이후 지난 시간" 을 지표로 만들어야 한다. 공개 통계에는 같은 값을 기준 시각으로 함께 내보내
 * 소개 사이트가 "○월 ○일 기준" 을 자동으로 표시할 수 있게 한다.
 */
@Slf4j
@Service
public class SyncFreshnessService {

    private final SyncRunRepository syncRunRepository;
    private final Environment environment;
    private final ConcurrentHashMap<SyncJob, AtomicReference<Cached>> cache = new ConcurrentHashMap<>();

    /** Prometheus 스크레이프마다 DB 를 때리지 않도록 잠깐 캐시한다. 테스트는 0 으로 끈다. */
    private final Duration cacheTtl;

    public SyncFreshnessService(SyncRunRepository syncRunRepository,
                                Environment environment,
                                MeterRegistry meterRegistry) {
        this.syncRunRepository = syncRunRepository;
        this.environment = environment;
        this.cacheTtl = Duration.ofSeconds(
                environment.getProperty("app.sync.freshness.cache-ttl-seconds", Long.class, 30L));

        for (SyncJob job : SyncJob.values()) {
            // 한 번도 성공하지 않은 작업은 NaN 이다. 0 으로 두면 "방금 성공" 과 구분되지 않는다.
            Gauge.builder("carecode.sync.last.success.age.seconds", this, self -> self.ageSeconds(job))
                    .description("마지막 성공 이후 경과 시간(초). 값이 없으면 한 번도 성공하지 않았다")
                    .tag("job", job.getCode())
                    .register(meterRegistry);
            Gauge.builder("carecode.sync.stale", this, self -> self.isStale(job) ? 1 : 0)
                    .description("신선도 기준을 넘겼는가 (1=넘김)")
                    .tag("job", job.getCode())
                    .register(meterRegistry);
        }
    }

    /** 이 작업이 마지막으로 데이터를 갱신한 시각. 한 번도 없으면 빈 값. */
    @Transactional(readOnly = true)
    public Optional<LocalDateTime> lastFreshAt(SyncJob job) {
        return snapshot(job).lastFreshAt();
    }

    /** 공개 통계의 "기준 시각". 여러 작업이 한 화면을 채우면 그중 가장 오래된 값을 쓴다(가장 보수적). */
    public Optional<LocalDateTime> lastFreshAt(SyncJob... jobs) {
        return Arrays.stream(jobs)
                .map(this::lastFreshAt)
                .flatMap(Optional::stream)
                .min(LocalDateTime::compareTo);
    }

    public boolean isStale(SyncJob job) {
        Optional<LocalDateTime> lastFreshAt = snapshot(job).lastFreshAt();
        if (lastFreshAt.isEmpty()) {
            // 한 번도 안 돌았다. 방금 배포한 환경에서도 참이라 알림 판단은 호출부에서 기동 시간과 함께 본다.
            return true;
        }
        return Duration.between(lastFreshAt.get(), LocalDateTime.now()).toHours() >= staleAfterHours(job);
    }

    public int staleAfterHours(SyncJob job) {
        return environment.getProperty("app.sync.freshness." + job.getCode(), Integer.class, job.getStaleAfterHours());
    }

    /** 관리자 화면·알림에서 쓰는 작업별 현재 상태. */
    @Transactional(readOnly = true)
    public List<JobFreshness> describeAll() {
        return Arrays.stream(SyncJob.values()).map(job -> {
            Snapshot snapshot = snapshot(job);
            SyncRun lastRun = snapshot.lastRun();
            return JobFreshness.builder()
                    .job(job.getCode())
                    .label(job.getLabel())
                    .dataFreshness(job.isDataFreshness())
                    .lastFreshAt(snapshot.lastFreshAt().orElse(null))
                    .ageHours(snapshot.lastFreshAt()
                            .map(at -> Duration.between(at, LocalDateTime.now()).toHours())
                            .orElse(null))
                    .staleAfterHours(staleAfterHours(job))
                    .stale(isStale(job))
                    .lastStatus(lastRun != null ? lastRun.getStatus().name() : null)
                    .lastFinishedAt(lastRun != null ? lastRun.getFinishedAt() : null)
                    .lastProcessed(lastRun != null ? lastRun.getProcessed() : null)
                    .lastFailed(lastRun != null ? lastRun.getFailed() : null)
                    .lastDetail(lastRun != null ? lastRun.getDetail() : null)
                    .build();
        }).toList();
    }

    private Double ageSeconds(SyncJob job) {
        return snapshot(job).lastFreshAt()
                .map(at -> (double) Duration.between(at, LocalDateTime.now()).toSeconds())
                .orElse(Double.NaN);
    }

    private Snapshot snapshot(SyncJob job) {
        AtomicReference<Cached> holder = cache.computeIfAbsent(job, j -> new AtomicReference<>());
        Cached cached = holder.get();
        if (!cacheTtl.isZero() && cached != null
                && Duration.between(cached.readAt(), LocalDateTime.now()).compareTo(cacheTtl) < 0) {
            return cached.snapshot();
        }
        Snapshot fresh = load(job);
        holder.set(new Cached(LocalDateTime.now(), fresh));
        return fresh;
    }

    private Snapshot load(SyncJob job) {
        try {
            return new Snapshot(
                    syncRunRepository.findLastFreshRun(job.getCode()).map(SyncRun::getFinishedAt),
                    syncRunRepository.findFirstByJobOrderByFinishedAtDesc(job.getCode()).orElse(null));
        } catch (RuntimeException e) {
            // 지표 수집이 장애 원인이 되면 안 된다.
            log.warn("작업 신선도 조회 실패 - job={}", job.getCode(), e);
            return new Snapshot(Optional.empty(), null);
        }
    }

    private record Snapshot(Optional<LocalDateTime> lastFreshAt, SyncRun lastRun) {
    }

    private record Cached(LocalDateTime readAt, Snapshot snapshot) {
    }

    @Getter
    @Builder
    public static class JobFreshness {
        private final String job;
        private final String label;
        private final boolean dataFreshness;
        private final LocalDateTime lastFreshAt;
        private final Long ageHours;
        private final int staleAfterHours;
        private final boolean stale;
        private final String lastStatus;
        private final LocalDateTime lastFinishedAt;
        private final Integer lastProcessed;
        private final Integer lastFailed;
        private final String lastDetail;
    }
}
