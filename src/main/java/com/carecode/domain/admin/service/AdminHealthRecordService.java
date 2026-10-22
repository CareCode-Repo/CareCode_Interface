package com.carecode.domain.admin.service;

import com.carecode.core.exception.HealthRecordNotFoundException;
import com.carecode.domain.health.dto.response.HealthRecordResponse;
import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.mapper.HealthRecordMapper;
import com.carecode.domain.health.repository.HealthRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민 건강기록 관리. 건강기록은 민감정보라 관리자가 대신 생성·수정하지 않고 조회와 삭제만 한다.
 *
 * <p>삭제는 하드 삭제다. 사용자 쪽 {@code HealthService#deleteHealthRecord} 와 같은 동작이며,
 * 엔티티의 {@code cascade = ALL, orphanRemoval = true} 로 첨부 행도 함께 지워진다.
 * (저장된 첨부 <b>파일</b>은 양쪽 모두 지우지 않는다 — 그건 첨부 저장소 쪽 과제다.)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminHealthRecordService {

    private final HealthRecordRepository healthRecordRepository;
    private final HealthRecordMapper healthRecordMapper;

    public Page<HealthRecordResponse> list(Pageable pageable) {
        return healthRecordRepository.findAll(pageable).map(healthRecordMapper::toResponse);
    }

    public HealthRecordResponse detail(Long id) {
        return healthRecordMapper.toResponse(require(id));
    }

    @Transactional
    public void delete(Long id) {
        healthRecordRepository.delete(require(id));
    }

    private HealthRecord require(Long id) {
        return healthRecordRepository.findById(id)
                .orElseThrow(() -> new HealthRecordNotFoundException(id));
    }
}
