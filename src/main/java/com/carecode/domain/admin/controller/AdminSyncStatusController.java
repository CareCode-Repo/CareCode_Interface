package com.carecode.domain.admin.controller;

import com.carecode.core.ops.sync.SyncFreshnessService;
import com.carecode.domain.careFacility.service.ForecastBacktestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 주기 작업 상태 조회.
 *
 * <p>"동기화가 돌고 있나" 를 확인할 방법이 로그뿐이었다. 배포 로그를 뒤지지 않고
 * 작업별 마지막 성공 시각과 기준 초과 여부를 한눈에 본다.
 * 같은 값이 Prometheus 지표({@code carecode.sync.last.success.age.seconds})로도 나간다.
 */
@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
@Tag(name = "어드민 - 주기 작업", description = "동기화·알림 작업 실행 상태")
public class AdminSyncStatusController {

    private final SyncFreshnessService freshnessService;
    private final ForecastBacktestService backtestService;

    @GetMapping("/status")
    @Operation(summary = "주기 작업 상태", description = "작업별 마지막 성공 시각, 경과 시간, 신선도 기준 초과 여부")
    public ResponseEntity<Map<String, Object>> status() {
        List<SyncFreshnessService.JobFreshness> jobs = freshnessService.describeAll();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobs", jobs);
        body.put("staleCount", jobs.stream().filter(SyncFreshnessService.JobFreshness::isStale).count());
        return ResponseEntity.ok(body);
    }

    /**
     * 예측 정확도 수동 측정.
     *
     * <p>스케줄러는 주 1회라, 새 관측이 들어온 뒤 결과를 바로 보고 싶을 때 쓴다.
     * 표본이 부족한 기간은 결과에 포함되지 않는다(없는 정확도를 만들어내지 않는다).
     */
    @org.springframework.web.bind.annotation.PostMapping("/forecast-accuracy/measure")
    @Operation(summary = "예측 정확도 측정 실행", description = "과거 관측으로 백테스트를 돌려 기간별 정확도를 다시 계산")
    public ResponseEntity<Map<String, Object>> measureForecastAccuracy() {
        var results = backtestService.runAll();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("measured", results.size());
        body.put("results", results.stream().map(r -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("horizonMonths", r.getHorizonMonths());
            row.put("samples", r.getSamples());
            row.put("facilities", r.getFacilities());
            row.put("actualRate", r.getActualRate());
            row.put("brierScore", r.getBrierScore());
            row.put("baselineBrierScore", r.getBaselineBrierScore());
            row.put("betterThanBaseline", r.betterThanBaseline());
            return row;
        }).toList());
        return ResponseEntity.ok(body);
    }
}
