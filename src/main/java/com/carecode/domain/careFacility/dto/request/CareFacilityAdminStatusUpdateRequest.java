package com.carecode.domain.careFacility.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자 상태 업데이트 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareFacilityAdminStatusUpdateRequest {
    @NotNull(message = "예약 ID는 필수입니다")
    private Long bookingId;
    
    @NotBlank(message = "상태는 필수입니다")
    private String status;
    
    private String adminNotes;
}

