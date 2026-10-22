package com.carecode.domain.admin.controller;

import com.carecode.domain.admin.service.AdminHospitalService;
import com.carecode.domain.health.dto.response.HospitalInfoResponse;
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

/** 어드민 병원 관리 API. 병원은 공공데이터에서 동기화되므로 조회와 삭제만 제공한다. */
@RestController
@RequestMapping("/api/admin/hospitals")
@RequiredArgsConstructor
@Tag(name = "어드민 - 병원", description = "관리자 전용 병원 관리 API")
public class AdminHospitalController {

    private final AdminHospitalService adminHospitalService;

    @GetMapping
    @Operation(summary = "병원 목록 조회")
    public ResponseEntity<Page<HospitalInfoResponse>> list(
            @PageableDefault(size = 50, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(adminHospitalService.list(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "병원 상세 조회")
    public ResponseEntity<HospitalInfoResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(adminHospitalService.detail(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "병원 삭제")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminHospitalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
