package com.carecode.domain.health.dto.response;

import com.carecode.domain.health.entity.HealthRecordAttachment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 건강기록 첨부파일 응답. */
@Getter
@Builder
public class AttachmentResponse {

    private final Long id;
    private final Long healthRecordId;
    private final String fileUrl;
    private final String fileName;
    private final String fileType;
    private final Long fileSize;
    private final String description;
    private final Integer displayOrder;
    private final LocalDateTime createdAt;

    public static AttachmentResponse from(HealthRecordAttachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .healthRecordId(attachment.getHealthRecord() != null
                        ? attachment.getHealthRecord().getId() : null)
                .fileUrl(attachment.getFileUrl())
                .fileName(attachment.getFileName())
                .fileType(attachment.getFileType())
                .fileSize(attachment.getFileSize())
                .description(attachment.getDescription())
                .displayOrder(attachment.getDisplayOrder())
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}
