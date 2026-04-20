package com.carecode.domain.health.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthRecordAttachmentResponse {
    private Long attachmentId;
    private Long recordId;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String description;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}
