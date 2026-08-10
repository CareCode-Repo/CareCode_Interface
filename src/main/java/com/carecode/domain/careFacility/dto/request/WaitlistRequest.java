package com.carecode.domain.careFacility.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** 대기 신청 기록. */
@Getter
@Setter
@NoArgsConstructor
public class WaitlistRequest {

    /** 미지정 시 최근 등록 자녀. */
    private Long childId;

    @Min(value = 1, message = "대기 순번은 1 이상이어야 합니다")
    @Max(value = 9999, message = "대기 순번이 너무 큽니다")
    private Integer waitNumber;

    /** 미지정 시 오늘. */
    private LocalDate appliedAt;

    @Size(max = 300, message = "메모는 300자 이하여야 합니다")
    private String note;
}
