package com.carecode.domain.health.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthRecordAttachmentRequest {
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String description;
    private Integer displayOrder;
}
