package com.carecode.domain.admin.controller;

import com.carecode.domain.community.dto.response.ReportResponse;
import com.carecode.domain.community.entity.Report;
import com.carecode.domain.community.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 어드민 신고 처리 API.
 */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@Tag(name = "어드민 - 신고 처리", description = "관리자 전용 커뮤니티 신고 처리 API")
public class AdminReportController {

    private final ModerationService moderationService;

    @GetMapping
    @Operation(summary = "미처리 신고 목록 조회")
    public ResponseEntity<Page<ReportResponse>> pendingReports(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(moderationService.getPendingReports(pageable));
    }

    @PatchMapping("/{reportId}")
    @Operation(summary = "신고 처리",
            description = "ACCEPTED 로 처리하면 대상 게시글·댓글이 숨김 처리됩니다.")
    public ResponseEntity<ReportResponse> resolve(
            @PathVariable Long reportId,
            @Parameter(description = "처리 결과 (ACCEPTED 또는 REJECTED)", required = true)
            @RequestParam Report.ReportStatus status,
            @Parameter(description = "처리 메모") @RequestParam(required = false) String note) {
        return ResponseEntity.ok(moderationService.resolve(reportId, status, note));
    }
}
