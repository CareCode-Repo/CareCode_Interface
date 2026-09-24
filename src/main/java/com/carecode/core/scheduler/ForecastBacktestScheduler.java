package com.carecode.core.scheduler;

import com.carecode.core.ops.sync.SyncJob;
import com.carecode.core.ops.sync.SyncRunTracker;
import com.carecode.domain.careFacility.service.ForecastBacktestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 입소 예측 정확도 측정. 주 1회.
 *
 * <p>정원 관측이 주 1회 들어오므로 그보다 자주 돌릴 이유가 없다. 시설 동기화가 끝난 뒤에 돌려
 * 그 주의 관측까지 검증에 넣는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ForecastBacktestScheduler {

    private final ForecastBacktestService backtestService;
    private final SyncRunTracker tracker;

    @Scheduled(cron = "${app.scheduler.forecast-backtest.cron:0 30 4 * * SUN}", zone = "Asia/Seoul")
    public void measureAccuracy() {
        tracker.track(SyncJob.FORECAST_BACKTEST, () -> backtestService.runAll());
    }
}
