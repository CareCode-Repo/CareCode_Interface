package com.carecode.domain.admin.controller;

import com.carecode.core.exception.PolicyNotFoundException;
import com.carecode.domain.admin.dto.AdminPolicyRequest;
import com.carecode.domain.admin.service.PolicyAdminService;
import com.carecode.domain.policy.dto.response.PolicyDto;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.mapper.PolicyMapper;
import com.carecode.domain.policy.repository.PolicyRepository;
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

/**
 * 어드민 정책 관리 API.
 */
@RestController
@RequestMapping("/api/admin/policies")
@RequiredArgsConstructor
@Tag(name = "어드민 - 정책", description = "관리자 전용 정책 관리 API")
public class AdminPolicyController {

    private final PolicyRepository policyRepository;
    private final PolicyMapper policyMapper;
    private final PolicyAdminService policyAdminService;

    @GetMapping
    @Operation(summary = "정책 목록 조회")
    public ResponseEntity<Page<PolicyDto>> list(
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(policyRepository.findAll(pageable).map(policyMapper::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "정책 상세 조회")
    public ResponseEntity<PolicyDto> detail(@PathVariable Long id) {
        return ResponseEntity.ok(policyMapper.toResponse(findPolicy(id)));
    }

    @PostMapping
    @Operation(summary = "정책 등록", description = "재배포 없이 새 정책을 추가합니다.")
    public ResponseEntity<PolicyDto> create(@Valid @RequestBody AdminPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyAdminService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "정책 수정", description = "수정 시 해당 정책의 캐시가 무효화됩니다.")
    public ResponseEntity<PolicyDto> update(@PathVariable Long id,
                                            @Valid @RequestBody AdminPolicyRequest request) {
        return ResponseEntity.ok(policyAdminService.update(id, request));
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
