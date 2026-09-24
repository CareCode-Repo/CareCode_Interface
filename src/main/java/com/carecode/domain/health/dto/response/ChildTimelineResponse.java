package com.carecode.domain.health.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 아이 한 명의 할 일을 시간 축 하나에 모은 것.
 *
 * <p>접종은 접종 화면, 검진은 기록 화면, 지원금 마감은 정책 화면에 흩어져 있었다. 부모가 "다음에 뭘
 * 해야 하나" 를 알려면 화면 세 곳을 돌아야 했고, 그래서 놓쳤다. 데이터는 이미 다 있으므로 합쳐서 준다.
 */
@Getter
@Builder
public class ChildTimelineResponse {

    private final Long childId;
    private final String childName;
    private final LocalDate birthDate;

    /** 조회 구간. 기준일(오늘)부터 몇 개월까지 본 결과인지. */
    private final LocalDate from;
    private final LocalDate to;

    /** 지난 항목 중 아직 하지 않은 것. 구간 앞이라도 놓친 건 보여 줘야 한다. */
    private final int overdueCount;
    private final int upcomingCount;

    private final List<TimelineItem> items;

    @Getter
    @Builder
    public static class TimelineItem {

        /** 기준 날짜. 구간이 있는 항목(검진)은 시작일을 쓴다. */
        private final LocalDate date;

        /** VACCINATION, CHECKUP, POLICY_DEADLINE, NEW_TERM */
        private final String type;

        /** OVERDUE(지났는데 안 함), UPCOMING(앞으로), DONE(완료), INFO(참고) */
        private final String status;

        private final String title;
        private final String description;

        /** 해당 도메인 상세로 이어 주기 위한 식별자. 없으면 null. */
        private final String referenceId;

        /** 그 날짜의 아이 월령. 화면에서 "12개월 무렵" 처럼 쓴다. */
        private final Integer ageMonths;
    }
}
