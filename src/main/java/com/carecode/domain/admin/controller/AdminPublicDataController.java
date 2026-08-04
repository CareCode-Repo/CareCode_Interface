package com.carecode.domain.admin.controller;

import com.carecode.core.client.sync.GovernmentBenefitSyncService;
import com.carecode.core.client.sync.NationwideChildcareFacilitySyncService;
import com.carecode.core.client.sync.SyncResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 공공데이터 수동 동기화 API.
 *
 * <p>정기 동기화는 {@code PublicDataSyncScheduler} 가 담당한다.
 * 이 엔드포인트는 즉시 반영이 필요할 때 쓰는 보조 수단이다.
 */
@RestController
@RequestMapping("/api/admin/public-data")
@RequiredArgsConstructor
@Tag(name = "어드민 - 공공데이터", description = "공공데이터 수동 동기화 API")
public class AdminPublicDataController {

    private final NationwideChildcareFacilitySyncService facilitySyncService;
    private final GovernmentBenefitSyncService benefitSyncService;

    @PostMapping("/facilities/sync")
    @Operation(summary = "전국 어린이집 동기화",
            description = "공공데이터포털에서 전국 어린이집 정보를 받아 시설 코드 기준으로 갱신합니다.")
    public ResponseEntity<Map<String, Object>> syncFacilities() {
        return ResponseEntity.ok(toResponse(facilitySyncService.sync()));
    }

    @PostMapping("/benefits/sync")
    @Operation(summary = "정부 지원 서비스 동기화",
            description = "보조금24 공공서비스 정보에서 육아 관련 서비스를 받아 정책으로 갱신합니다.")
    public ResponseEntity<Map<String, Object>> syncBenefits() {
        return ResponseEntity.ok(toResponse(benefitSyncService.sync()));
    }

    private Map<String, Object> toResponse(SyncResult result) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("provider", result.getProvider());
        body.put("resource", result.getResource());
        body.put("completed", result.isCompleted());
        body.put("created", result.getCreated());
        body.put("updated", result.getUpdated());
        body.put("failed", result.getFailed());
        body.put("pagesProcessed", result.getPagesProcessed());
        body.put("stoppedReason", result.getStoppedReason());
        return body;
    }
}
