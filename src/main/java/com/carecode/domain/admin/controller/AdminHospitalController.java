package com.carecode.domain.admin.controller;

import com.carecode.core.exception.HospitalNotFoundException;
import com.carecode.domain.health.dto.response.HospitalInfoResponse;
import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.health.mapper.HospitalMapper;
import com.carecode.domain.health.repository.HospitalRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * 어드민 병원 관리 API.
 */
@RestController
@RequestMapping("/api/admin/hospitals")
@RequiredArgsConstructor
@Tag(name = "어드민 - 병원", description = "관리자 전용 병원 관리 API")
public class AdminHospitalController {

    private final HospitalRepository hospitalRepository;
    private final HospitalMapper hospitalMapper;

    @GetMapping
    @Operation(summary = "병원 목록 조회")
    public ResponseEntity<Page<HospitalInfoResponse>> list(
            @PageableDefault(size = 50, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(hospitalRepository.findAll(pageable).map(hospitalMapper::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "병원 상세 조회")
    public ResponseEntity<HospitalInfoResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(hospitalMapper.toResponse(findHospital(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "병원 삭제")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        hospitalRepository.delete(findHospital(id));
        return ResponseEntity.noContent().build();
    }

    private Hospital findHospital(Long id) {
        return hospitalRepository.findById(id)
                .orElseThrow(() -> new HospitalNotFoundException(id));
    }
}
