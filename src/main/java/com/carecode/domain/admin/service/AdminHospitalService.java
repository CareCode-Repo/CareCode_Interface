package com.carecode.domain.admin.service;

import com.carecode.core.exception.HospitalNotFoundException;
import com.carecode.domain.health.dto.response.HospitalInfoResponse;
import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.health.mapper.HospitalMapper;
import com.carecode.domain.health.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 어드민 병원 관리. 병원은 공공데이터에서 동기화되므로 관리자는 조회와 삭제만 한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminHospitalService {

    private final HospitalRepository hospitalRepository;
    private final HospitalMapper hospitalMapper;

    public Page<HospitalInfoResponse> list(Pageable pageable) {
        return hospitalRepository.findAll(pageable).map(hospitalMapper::toResponse);
    }

    public HospitalInfoResponse detail(Long id) {
        return hospitalMapper.toResponse(require(id));
    }

    @Transactional
    public void delete(Long id) {
        hospitalRepository.delete(require(id));
    }

    private Hospital require(Long id) {
        return hospitalRepository.findById(id)
                .orElseThrow(() -> new HospitalNotFoundException("병원을 찾을 수 없습니다. ID: " + id));
    }
}
