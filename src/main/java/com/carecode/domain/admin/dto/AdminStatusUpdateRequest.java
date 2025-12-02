package com.carecode.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 관리자용 예약 상태 변경 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatusUpdateRequest {
    private String status;
    private String reason; // 취소 사유 등
    private String adminNote; // 관리자 메모
}
