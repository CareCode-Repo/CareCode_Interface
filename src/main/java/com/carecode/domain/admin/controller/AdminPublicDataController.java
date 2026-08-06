package com.carecode.domain.admin.controller;

import com.carecode.core.client.sync.GovernmentBenefitSyncService;
import com.carecode.core.client.sync.KindergartenSyncService;
import com.carecode.core.client.sync.NationwideChildcareFacilitySyncService;
import com.carecode.core.client.sync.PediatricHospitalSyncService;
import com.carecode.core.client.sync.SyncResult;
import com.carecode.core.geocoding.FacilityGeocodingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 공공데이터 수동 동기화 API. */
@RestController
@RequestMapping("/api/admin/public-data")
@RequiredArgsConstructor
@Tag(name = "어드민 - 공공데이터", description = "공공데이터 수동 동기화 API")
public class AdminPublicDataController {

    private final NationwideChildcareFacilitySyncService facilitySyncService;
    private final KindergartenSyncService kindergartenSyncService;
    private final GovernmentBenefitSyncService benefitSyncService;
    private final PediatricHospitalSyncService hospitalSyncService;
    private final FacilityGeocodingService geocodingService;

    @PostMapping("/facilities/sync")
    @Operation(summary = "전국 어린이집 동기화", description = "시설 코드 기준으로 갱신")
    public ResponseEntity<Map<String, Object>> syncFacilities() {
        return ResponseEntity.ok(toResponse(facilitySyncService.sync()));
    }

    @PostMapping("/kindergartens/sync")
    @Operation(summary = "전국 유치원 동기화", description = "유치원명·주소 기준으로 갱신")
    public ResponseEntity<Map<String, Object>> syncKindergartens() {
        return ResponseEntity.ok(toResponse(kindergartenSyncService.sync()));
    }

    @PostMapping("/benefits/sync")
    @Operation(summary = "정부 지원 서비스 동기화", description = "육아 관련 서비스만 정책으로 갱신")
    public ResponseEntity<Map<String, Object>> syncBenefits() {
        return ResponseEntity.ok(toResponse(benefitSyncService.sync()));
    }

    @PostMapping("/hospitals/sync")
    @Operation(summary = "소아청소년과 병원 동기화", description = "요양기호 기준으로 갱신")
    public ResponseEntity<Map<String, Object>> syncHospitals() {
        return ResponseEntity.ok(toResponse(hospitalSyncService.sync()));
    }

    @PostMapping("/facilities/geocode")
    @Operation(summary = "시설 좌표 보정", description = "좌표 없는 시설의 주소를 좌표로 변환")
    public ResponseEntity<Map<String, Object>> geocode() {
        var result = geocodingService.fillMissingCoordinates();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("resolved", result.getResolved());
        body.put("failed", result.getFailed());
        body.put("remaining", result.getRemaining());
        body.put("skippedReason", result.getSkippedReason());
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> toResponse(SyncResult result) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("provider", result.getProvider());
        body.put("resource", result.getResource());
        body.put("completed", result.isCompleted());
        body.put("created", result.getCreated());
        body.put("updated", result.getUpdated());
        body.put("failed", result.getFailed());
        body.put("skipped", result.getSkipped());
        body.put("pagesProcessed", result.getPagesProcessed());
        body.put("stoppedReason", result.getStoppedReason());
        return body;
    }
}
