package com.carecode.domain.admin.controller;

import com.carecode.core.ops.sync.SyncFreshnessService;
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

    @GetMapping("/status")
    @Operation(summary = "주기 작업 상태", description = "작업별 마지막 성공 시각, 경과 시간, 신선도 기준 초과 여부")
    public ResponseEntity<Map<String, Object>> status() {
        List<SyncFreshnessService.JobFreshness> jobs = freshnessService.describeAll();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobs", jobs);
        body.put("staleCount", jobs.stream().filter(SyncFreshnessService.JobFreshness::isStale).count());
        return ResponseEntity.ok(body);
    }
}
