package com.carecode.domain.admin.controller;

import com.carecode.core.exception.HealthRecordNotFoundException;
import com.carecode.domain.health.dto.response.HealthRecordResponse;
import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.mapper.HealthRecordMapper;
import com.carecode.domain.health.repository.HealthRecordRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * 어드민 건강기록 관리 API.
 *
 * <p>건강기록은 민감정보다. 관리자가 대신 생성·수정하지 않고 조회와 삭제만 제공한다.
 */
@RestController
@RequestMapping("/api/admin/health/records")
@RequiredArgsConstructor
@Tag(name = "어드민 - 건강기록", description = "관리자 전용 건강기록 관리 API")
public class AdminHealthController {

    private final HealthRecordRepository healthRecordRepository;
    private final HealthRecordMapper healthRecordMapper;

    @GetMapping
    @Operation(summary = "건강기록 목록 조회")
    public ResponseEntity<Page<HealthRecordResponse>> list(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(healthRecordRepository.findAll(pageable).map(healthRecordMapper::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "건강기록 상세 조회")
    public ResponseEntity<HealthRecordResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(healthRecordMapper.toResponse(findRecord(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "건강기록 삭제")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        healthRecordRepository.delete(findRecord(id));
        return ResponseEntity.noContent().build();
    }

    private HealthRecord findRecord(Long id) {
        return healthRecordRepository.findById(id)
                .orElseThrow(() -> new HealthRecordNotFoundException("건강 기록을 찾을 수 없습니다: " + id));
    }
}
