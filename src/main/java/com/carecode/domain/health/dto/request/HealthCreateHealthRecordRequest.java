package com.carecode.domain.health.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 건강 기록 생성 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthCreateHealthRecordRequest {
    @NotBlank(message = "아동 ID는 필수입니다")
    private String childId;
    
    @NotBlank(message = "기록 타입은 필수입니다")
    private String recordType;
    
    @NotBlank(message = "제목은 필수입니다")
    private String title;
    
    private String description;
    
    @NotNull(message = "기록 날짜는 필수입니다")
    private LocalDateTime recordDate;
    
    private LocalDateTime nextDate;
    private String location;
    private String doctorName;
}

