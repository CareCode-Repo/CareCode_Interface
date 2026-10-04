package com.carecode.domain.facility.dto.response;

import com.carecode.domain.facility.entity.FacilityWaitlist;

import java.time.LocalDate;

/**
 * 내가 등록한 대기 기록 한 줄.
 *
 * <p>{@code status} 와 {@code statusName} 을 함께 준다. 화면이 코드값을 직접 번역하면
 * 서버가 상태를 추가할 때 화면에 빈칸이 생긴다.
 */
public record WaitlistEntryResponse(
        Long waitlistId,
        Long facilityId,
        Integer waitNumber,
        LocalDate appliedAt,
        String status,
        String statusName,
        long waitedDays) {

    public static WaitlistEntryResponse from(FacilityWaitlist entry) {
        return new WaitlistEntryResponse(
                entry.getId(),
                entry.getFacilityId(),
                entry.getWaitNumber(),
                entry.getAppliedAt(),
                entry.getStatus().name(),
                entry.getStatus().getDisplayName(),
                entry.waitedDays());
    }
}
