package com.carecode.domain.health.repository;

import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.entity.HealthRecordAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealthRecordAttachmentRepository extends JpaRepository<HealthRecordAttachment, Long> {
    List<HealthRecordAttachment> findByHealthRecordAndIsActiveTrueOrderByDisplayOrderAscCreatedAtDesc(HealthRecord healthRecord);
}
