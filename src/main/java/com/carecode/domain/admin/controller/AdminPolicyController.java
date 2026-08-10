package com.carecode.domain.admin.controller;

import com.carecode.core.exception.PolicyNotFoundException;
import com.carecode.domain.admin.dto.AdminPolicyDetailResponse;
import com.carecode.domain.admin.dto.AdminPolicyRequest;
import com.carecode.domain.admin.service.PolicyAdminService;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 어드민 정책 관리 API. */
@RestController
@RequestMapping("/api/admin/policies")
@RequiredArgsConstructor
@Tag(name = "어드민 - 정책", description = "관리자 전용 정책 관리 API")
public class AdminPolicyController {

    private final PolicyRepository policyRepository;
    private final PolicyAdminService policyAdminService;

    @GetMapping
    @Operation(summary = "정책 목록 조회",
            description = "수정 요청과 1:1 로 대응하는 원본 값을 반환합니다 (사용자용 PolicyDto 는 가공된 값이라 수정에 쓸 수 없음)")
    public ResponseEntity<Page<AdminPolicyDetailResponse>> list(
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(policyRepository.findAll(pageable).map(AdminPolicyDetailResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "정책 상세 조회")
    public ResponseEntity<AdminPolicyDetailResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(AdminPolicyDetailResponse.from(findPolicy(id)));
    }

    @PostMapping
    @Operation(summary = "정책 등록", description = "재배포 없이 새 정책 추가")
    public ResponseEntity<AdminPolicyDetailResponse> create(@Valid @RequestBody AdminPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyAdminService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "정책 전체 교체",
            description = "요청에 담긴 값으로 전체를 덮어씁니다. 보내지 않은 항목은 null 이 되므로, "
                    + "일부만 고칠 때는 PATCH 를 쓰세요")
    public ResponseEntity<AdminPolicyDetailResponse> update(@PathVariable Long id,
                                            @Valid @RequestBody AdminPolicyRequest request) {
        return ResponseEntity.ok(policyAdminService.update(id, request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "정책 부분 수정",
            description = "요청 JSON 에 담긴 키만 반영합니다. 키가 없으면 기존 값을 유지하고, "
                    + "키가 있는데 값이 null 이면 해당 항목을 비웁니다")
    public ResponseEntity<AdminPolicyDetailResponse> patch(@PathVariable Long id,
                                                           @RequestBody JsonNode body) {
        return ResponseEntity.ok(policyAdminService.patch(id, body));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "정책 삭제")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        policyAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Policy findPolicy(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new PolicyNotFoundException("정책을 찾을 수 없습니다: " + id));
    }
}
