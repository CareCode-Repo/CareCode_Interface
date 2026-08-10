package com.carecode.domain.admin.controller;

import com.carecode.core.analytics.AnalyticsService;
import com.carecode.core.analytics.dto.FunnelResponse;
import com.carecode.core.analytics.dto.RetentionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/** 지표 조회 API. */
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "어드민 - 지표", description = "퍼널·리텐션 지표")
public class AdminAnalyticsController {

    private static final int DEFAULT_RANGE_DAYS = 30;

    private final AnalyticsService analyticsService;

    @GetMapping("/funnel")
    @Operation(summary = "온보딩 퍼널 조회", description = "단계별 도달 사용자 수와 전환율")
    public ResponseEntity<FunnelResponse> funnel(
            @Parameter(description = "시작일 (기본: 30일 전)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "종료일 (기본: 오늘)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_RANGE_DAYS);
        return ResponseEntity.ok(analyticsService.funnel(start, end));
    }

    @GetMapping("/notification-funnel")
    @Operation(summary = "알림 효과 퍼널", description = "발송 → 클릭 → 신청 전환율")
    public ResponseEntity<FunnelResponse> notificationFunnel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_RANGE_DAYS);
        return ResponseEntity.ok(analyticsService.notificationFunnel(start, end));
    }

    @GetMapping("/retention")
    @Operation(summary = "코호트 리텐션 조회", description = "가입일 기준 D1/D7/D30 잔존율")
    public ResponseEntity<RetentionResponse> retention(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_RANGE_DAYS);
        return ResponseEntity.ok(analyticsService.retention(start, end));
    }

    @GetMapping("/events")
    @Operation(summary = "이벤트 발생 건수", description = "종류별 집계")
    public ResponseEntity<Map<String, Long>> events(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_RANGE_DAYS);
        return ResponseEntity.ok(analyticsService.eventCounts(start, end));
    }
}
