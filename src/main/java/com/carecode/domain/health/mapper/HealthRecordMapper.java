package com.carecode.domain.health.mapper;

import com.carecode.core.util.RequestMapper;
import com.carecode.core.util.ResponseMapper;
import com.carecode.domain.health.dto.HealthRequest;
import com.carecode.domain.health.dto.HealthResponse;
import com.carecode.domain.health.entity.HealthRecord;
import org.springframework.stereotype.Component;

/**
 * HealthRecord 변환용 공통 매퍼
 */
@Component
public class HealthRecordMapper implements RequestMapper<HealthRequest.CreateHealthRecord, HealthRecord>, ResponseMapper<HealthRecord, HealthResponse.HealthRecordResponse> {

    @Override
    public HealthRecord toEntity(HealthRequest.CreateHealthRecord request) {
        HealthRecord.HealthRecordBuilder builder = HealthRecord.builder()
                .recordType(HealthRecord.RecordType.valueOf(request.getRecordType()))
                .title(request.getTitle())
                .description(request.getDescription())
                .recordDate(request.getRecordDate() != null ? request.getRecordDate().toLocalDate() : null)
                .nextDate(request.getNextDate() != null ? request.getNextDate().toLocalDate() : null)
                .location(request.getLocation())
                .doctorName(request.getDoctorName())
                .isCompleted(false);
        return builder.build();
    }

    @Override
    public HealthResponse.HealthRecordResponse toResponse(HealthRecord record) {
        return HealthResponse.HealthRecordResponse.builder()
                .id(record.getId())
                .childId(record.getChild() != null ? record.getChild().getId().toString() : null)
                .childName(record.getChild() != null ? record.getChild().getName() : null)
                .userId(record.getUser().getId().toString())
                .recordType(record.getRecordType().toString())
                .title(record.getTitle())
                .description(record.getDescription())
                .recordDate(record.getRecordDate() != null ? record.getRecordDate().toString() : null)
                .nextDate(record.getNextDate() != null ? record.getNextDate().toString() : null)
                .location(record.getLocation())
                .doctorName(record.getDoctorName())
                .hospitalName(record.getHospitalName())
                .height(record.getHeight())
                .weight(record.getWeight())
                .temperature(record.getTemperature())
                .bloodPressure(record.getBloodPressure())
                .pulseRate(record.getPulseRate())
                .vaccineName(record.getVaccineName())
                .isCompleted(record.getIsCompleted())
                .createdAt(record.getCreatedAt() != null ? record.getCreatedAt().toString() : null)
                .updatedAt(record.getUpdatedAt() != null ? record.getUpdatedAt().toString() : null)
                .build();
    }
}


