package com.carecode.domain.admin.controller;

import com.carecode.domain.admin.service.AdminHealthRecordService;
import com.carecode.domain.health.dto.response.HealthRecordResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 어드민 건강기록 관리 API. 건강기록은 민감정보다. 관리자가 대신 생성·수정하지 않고 조회와 삭제만 제공한다. */
@RestController
@RequestMapping("/api/admin/health/records")
@RequiredArgsConstructor
@Tag(name = "어드민 - 건강기록", description = "관리자 전용 건강기록 관리 API")
public class AdminHealthController {

    private final AdminHealthRecordService adminHealthRecordService;

    @GetMapping
    @Operation(summary = "건강기록 목록 조회")
    public ResponseEntity<Page<HealthRecordResponse>> list(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminHealthRecordService.list(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "건강기록 상세 조회")
    public ResponseEntity<HealthRecordResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(adminHealthRecordService.detail(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "건강기록 삭제")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminHealthRecordService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
