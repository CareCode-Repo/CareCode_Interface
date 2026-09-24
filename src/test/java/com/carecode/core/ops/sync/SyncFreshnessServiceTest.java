package com.carecode.core.ops.sync;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.env.MockEnvironment;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("데이터 신선도 판단")
class SyncFreshnessServiceTest {

    @Mock SyncRunRepository syncRunRepository;

    private MockEnvironment environment;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
    }

    private SyncFreshnessService service() {
        return new SyncFreshnessService(syncRunRepository, environment, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("기준 시간 안에 성공했으면 낡지 않았다")
    void freshWithinThreshold() {
        givenLastFresh(SyncJob.GOVERNMENT_BENEFITS, LocalDateTime.now().minusHours(5));

        assertThat(service().isStale(SyncJob.GOVERNMENT_BENEFITS)).isFalse();
    }

    @Test
    @DisplayName("기준 시간을 넘기면 낡았다 (일간 작업 36시간)")
    void staleBeyondThreshold() {
        givenLastFresh(SyncJob.GOVERNMENT_BENEFITS, LocalDateTime.now().minusHours(40));

        SyncFreshnessService service = service();
        assertThat(service.isStale(SyncJob.GOVERNMENT_BENEFITS)).isTrue();
        assertThat(service.staleAfterHours(SyncJob.GOVERNMENT_BENEFITS)).isEqualTo(36);
    }

    @Test
    @DisplayName("주 1회 작업은 한 번 건너뛴 정도는 견딘다 (8일 기준)")
    void weeklyJobToleratesOneMiss() {
        givenLastFresh(SyncJob.CHILDCARE_FACILITIES, LocalDateTime.now().minusDays(7).minusHours(2));

        assertThat(service().isStale(SyncJob.CHILDCARE_FACILITIES)).isFalse();

        givenLastFresh(SyncJob.CHILDCARE_FACILITIES, LocalDateTime.now().minusDays(9));
        assertThat(service().isStale(SyncJob.CHILDCARE_FACILITIES)).isTrue();
    }

    @Test
    @DisplayName("성공 기록이 없으면 낡은 것으로 본다")
    void noRunIsStale() {
        when(syncRunRepository.findLastFreshRun(anyString())).thenReturn(Optional.empty());
        when(syncRunRepository.findFirstByJobOrderByFinishedAtDesc(anyString())).thenReturn(Optional.empty());

        SyncFreshnessService service = service();
        assertThat(service.isStale(SyncJob.CHILDCARE_FACILITIES)).isTrue();
        assertThat(service.lastFreshAt(SyncJob.CHILDCARE_FACILITIES)).isEmpty();
    }

    @Test
    @DisplayName("기준은 설정으로 덮어쓸 수 있다")
    void thresholdIsConfigurable() {
        environment.setProperty("app.sync.freshness.government-benefits", "72");
        givenLastFresh(SyncJob.GOVERNMENT_BENEFITS, LocalDateTime.now().minusHours(40));

        assertThat(service().isStale(SyncJob.GOVERNMENT_BENEFITS)).isFalse();
    }

    @Test
    @DisplayName("한 화면을 여러 작업이 채우면 가장 오래된 시각을 기준으로 준다")
    void oldestAmongJobs() {
        LocalDateTime older = LocalDateTime.now().minusDays(3);
        givenLastFresh(SyncJob.CHILDCARE_FACILITIES, older);
        givenLastFresh(SyncJob.KINDERGARTENS, LocalDateTime.now().minusHours(1));

        assertThat(service().lastFreshAt(SyncJob.CHILDCARE_FACILITIES, SyncJob.KINDERGARTENS))
                .contains(older);
    }

    @Test
    @DisplayName("조회가 실패해도 예외를 던지지 않는다 — 지표 수집이 장애 원인이 되면 안 된다")
    void repositoryFailureIsContained() {
        when(syncRunRepository.findLastFreshRun(anyString())).thenThrow(new RuntimeException("DB 연결 끊김"));

        assertThat(service().lastFreshAt(SyncJob.CHILDCARE_FACILITIES)).isEmpty();
    }

    @Test
    @DisplayName("상태 목록은 모든 작업을 담고 데이터 작업과 알림 작업을 구분한다")
    void describeAllCoversEveryJob() {
        givenLastFresh(SyncJob.CHILDCARE_FACILITIES, LocalDateTime.now().minusHours(1));

        var jobs = service().describeAll();

        assertThat(jobs).hasSize(SyncJob.values().length);
        assertThat(jobs).anyMatch(j -> j.getJob().equals("childcare-facilities") && j.isDataFreshness());
        assertThat(jobs).anyMatch(j -> j.getJob().equals("policy-deadline-notice") && !j.isDataFreshness());
    }

    private void givenLastFresh(SyncJob job, LocalDateTime finishedAt) {
        SyncRun run = SyncRun.builder()
                .job(job.getCode())
                .status(SyncRun.Status.SUCCESS)
                .startedAt(finishedAt.minusMinutes(1))
                .finishedAt(finishedAt)
                .durationMillis(60_000)
                .processed(10)
                .failed(0)
                .build();
        when(syncRunRepository.findLastFreshRun(job.getCode())).thenReturn(Optional.of(run));
        when(syncRunRepository.findFirstByJobOrderByFinishedAtDesc(job.getCode())).thenReturn(Optional.of(run));
    }
}
