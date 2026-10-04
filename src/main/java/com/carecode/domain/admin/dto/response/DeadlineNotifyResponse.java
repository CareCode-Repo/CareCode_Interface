package com.carecode.domain.admin.dto.response;

import com.carecode.domain.policy.service.PolicyDeadlineNotifier.DeadlineNotifyResult;

/** 마감 임박 알림 실행 결과. 대상 정책 수와 실제 발송 수를 따로 센다. */
public record DeadlineNotifyResponse(
        int policiesDueSoon,
        int notificationsSent) {

    public static DeadlineNotifyResponse from(DeadlineNotifyResult result) {
        return new DeadlineNotifyResponse(result.getPoliciesDueSoon(), result.getNotificationsSent());
    }
}
