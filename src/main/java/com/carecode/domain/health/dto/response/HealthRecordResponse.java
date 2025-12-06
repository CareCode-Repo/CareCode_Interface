package com.carecode.domain.health.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 건강 기록 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthRecordResponse {
    private Long id;
    private String childId;
    private String childName;
    private String userId;
    private String recordType;
    private String title;
    private String description;
    private String recordDate;
    private String nextDate;
    private String location;
    private String doctorName;
    private String hospitalName;
    private Double height;
    private Double weight;
    private Double temperature;
    private String bloodPressure;
    private Integer pulseRate;
    private String vaccineName;
    private Boolean isCompleted;
    private String createdAt;
    private String updatedAt;
}

