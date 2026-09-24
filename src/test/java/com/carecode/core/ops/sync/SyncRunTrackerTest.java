package com.carecode.core.ops.sync;

import com.carecode.core.client.sync.SyncResult;
import com.carecode.core.ops.OperationalAlerter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 주기 작업이 조용히 실패하지 않게 하는 것이 이 클래스의 목적이다.
 * 로그만 남기던 예전 동작에서는 예외로 죽은 실행이 아무 기록도 남기지 않았다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("주기 작업 이력 기록")
class SyncRunTrackerTest {

    @Mock SyncRunRepository syncRunRepository;
    @Mock OperationalAlerter alerter;
    @InjectMocks SyncRunTracker tracker;

    @Test
    @DisplayName("정상 완료는 SUCCESS 로 남고 건수를 기록한다")
    void success() {
        SyncResult result = new SyncResult("datagokr", "facilities");
        result.countCreated();
        result.countUpdated();
        result.countUpdated();

        tracker.track(SyncJob.CHILDCARE_FACILITIES, () -> result);

        SyncRun saved = captureSaved();
        assertThat(saved.getStatus()).isEqualTo(SyncRun.Status.SUCCESS);
        assertThat(saved.getProcessed()).isEqualTo(3);
        assertThat(saved.getFailed()).isZero();
        assertThat(saved.getJob()).isEqualTo("childcare-facilities");
        verify(alerter, never()).alert(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("일부 실패는 PARTIAL 로 남기고 알린다 — 데이터는 갱신됐으므로 신선도는 인정한다")
    void partial() {
        SyncResult result = new SyncResult("datagokr", "facilities");
        result.countCreated();
        result.countFailed();

        tracker.track(SyncJob.CHILDCARE_FACILITIES, () -> result);

        SyncRun saved = captureSaved();
        assertThat(saved.getStatus()).isEqualTo(SyncRun.Status.PARTIAL);
        assertThat(saved.getStatus().countsAsFresh()).isTrue();
        assertThat(saved.getFailed()).isEqualTo(1);
        verify(alerter).alert(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("중단된 실행은 INCOMPLETE 이고 신선도로 인정하지 않는다")
    void incomplete() {
        SyncResult result = new SyncResult("datagokr", "facilities");
        result.stop("일일 호출 한도 초과");

        tracker.track(SyncJob.GOVERNMENT_BENEFITS, () -> result);

        SyncRun saved = captureSaved();
        assertThat(saved.getStatus()).isEqualTo(SyncRun.Status.INCOMPLETE);
        assertThat(saved.getStatus().countsAsFresh()).isFalse();
        assertThat(saved.getDetail()).contains("일일 호출 한도 초과");
        verify(alerter).alert(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("예외로 죽어도 FAILED 로 남기고 알린 뒤 삼킨다 — 한 작업 실패가 다음 작업을 막지 않는다")
    void exceptionIsRecordedAndSwallowed() {
        assertThatCode(() -> tracker.track(SyncJob.KINDERGARTENS, () -> {
            throw new IllegalStateException("공공데이터 응답 파싱 실패");
        })).doesNotThrowAnyException();

        SyncRun saved = captureSaved();
        assertThat(saved.getStatus()).isEqualTo(SyncRun.Status.FAILED);
        assertThat(saved.getDetail()).contains("공공데이터 응답 파싱 실패");
        verify(alerter).alert(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("값을 돌려주지 않는 알림 작업도 기록한다")
    void runnableJob() {
        tracker.track(SyncJob.POLICY_DEADLINE_NOTICE, () -> {
        });

        assertThat(captureSaved().getStatus()).isEqualTo(SyncRun.Status.SUCCESS);
    }

    @Test
    @DisplayName("이력 저장이 실패해도 작업 결과를 망가뜨리지 않는다")
    void recordFailureDoesNotPropagate() {
        when(syncRunRepository.save(any())).thenThrow(new RuntimeException("DB 연결 끊김"));

        assertThatCode(() -> tracker.track(SyncJob.CHILDCARE_FACILITIES,
                () -> new SyncResult("datagokr", "facilities"))).doesNotThrowAnyException();
    }

    private SyncRun captureSaved() {
        ArgumentCaptor<SyncRun> captor = ArgumentCaptor.forClass(SyncRun.class);
        verify(syncRunRepository).save(captor.capture());
        return captor.getValue();
    }
}
