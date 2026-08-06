package com.carecode.domain.careFacility.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.domain.careFacility.dto.request.WaitlistRequest;
import com.carecode.domain.careFacility.dto.response.WaitlistStatsResponse;
import com.carecode.domain.careFacility.entity.FacilityWaitlist;
import com.carecode.domain.careFacility.service.FacilityWaitlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 어린이집 대기 기록. 실제 대기 기간은 공공데이터에 없어 사용자에게서만 얻는다. */
@RestController
@RequestMapping("/facilities")
@RequiredArgsConstructor
@Tag(name = "육아 시설", description = "육아 시설 정보 및 검색 API")
public class FacilityWaitlistController {

    private final FacilityWaitlistService waitlistService;

    @PostMapping("/{facilityId}/waitlist")
    @LogExecutionTime
    @Operation(summary = "대기 신청 기록", description = "대기 순번과 신청일을 남깁니다")
    public ResponseEntity<Map<String, Object>> register(
            @Parameter(description = "시설 ID", required = true) @PathVariable Long facilityId,
            @Valid @RequestBody WaitlistRequest request) {

        Long id = waitlistService.register(facilityId, request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("waitlistId", id);
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/waitlist/{waitlistId}")
    @LogExecutionTime
    @Operation(summary = "대기 결과 기록", description = "입소 또는 포기를 남겨 대기 기간을 확정합니다")
    public ResponseEntity<Void> resolve(
            @PathVariable Long waitlistId,
            @Parameter(description = "ADMITTED 또는 GAVE_UP", required = true) @RequestParam String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate resolvedAt,
            @RequestParam(required = false) String note) {

        waitlistService.resolve(waitlistId, status, resolvedAt, note);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/waitlist/me")
    @LogExecutionTime
    @Operation(summary = "내 대기 목록", description = "등록한 대기 기록 조회")
    public ResponseEntity<List<Map<String, Object>>> myWaitlists() {
        List<Map<String, Object>> rows = waitlistService.getMyWaitlists().stream()
                .map(this::toSummary)
                .toList();
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/{facilityId}/waitlist/stats")
    @LogExecutionTime
    @Operation(summary = "실제 대기 기간 통계", description = "입소한 사람들의 기록 기반")
    public ResponseEntity<WaitlistStatsResponse> stats(@PathVariable Long facilityId) {
        return ResponseEntity.ok(waitlistService.getStats(facilityId));
    }

    private Map<String, Object> toSummary(FacilityWaitlist entry) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("waitlistId", entry.getId());
        row.put("facilityId", entry.getFacilityId());
        row.put("waitNumber", entry.getWaitNumber());
        row.put("appliedAt", entry.getAppliedAt());
        row.put("status", entry.getStatus().name());
        row.put("statusName", entry.getStatus().getDisplayName());
        row.put("waitedDays", entry.waitedDays());
        return row;
    }
}
