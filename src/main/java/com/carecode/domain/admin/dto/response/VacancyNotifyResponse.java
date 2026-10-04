package com.carecode.domain.admin.dto.response;

import com.carecode.domain.facility.service.FacilityVacancyNotifier.VacancyNotifyResult;

/**
 * 빈자리 알림 실행 결과.
 *
 * <p>확인한 시설 수까지 주는 이유: 발송 0 건일 때 "대기자가 없어서" 인지 "자리가 안 나서" 인지
 * 구분해야 한다. 발송 수만 보면 둘 다 0 으로 같다.
 */
public record VacancyNotifyResponse(
        int facilitiesChecked,
        int facilitiesWithVacancy,
        int notificationsSent) {

    public static VacancyNotifyResponse from(VacancyNotifyResult result) {
        return new VacancyNotifyResponse(
                result.getFacilitiesChecked(), result.getFacilitiesWithVacancy(), result.getNotificationsSent());
    }
}
