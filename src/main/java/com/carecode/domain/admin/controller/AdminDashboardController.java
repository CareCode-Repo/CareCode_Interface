package com.carecode.domain.admin.controller;

import com.carecode.domain.admin.dto.response.AdminDashboardResponse;
import com.carecode.domain.admin.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 어드민 대시보드 API. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "어드민 - 대시보드", description = "관리자 대시보드 요약 API")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard")
    @Operation(summary = "대시보드 요약 조회", description = "전체 건수, 최근 활동, 가입자 추이 반환")
    public ResponseEntity<AdminDashboardResponse> dashboard() {
        return ResponseEntity.ok(adminDashboardService.summary());
    }
}
