package com.carecode.core.ops.sync;

import lombok.Getter;

/**
 * 신선도를 추적하는 주기 작업 목록.
 *
 * <p>{@code staleAfterHours} 는 "이 시간 안에 한 번은 성공했어야 한다" 는 기준이다.
 * 주 1회 작업은 한 번 건너뛴 것까지는 견디되 두 번은 넘기지 않도록 주기보다 약간 길게 잡는다
 * (주간 168시간 → 192시간). 기준은 {@code app.sync.freshness.<code>} 로 덮어쓸 수 있다.
 */
@Getter
public enum SyncJob {

    CHILDCARE_FACILITIES("childcare-facilities", "전국 어린이집", 192, true),
    KINDERGARTENS("kindergartens", "전국 유치원", 192, true),
    GOVERNMENT_BENEFITS("government-benefits", "정부 지원 서비스", 36, true),
    PEDIATRIC_HOSPITALS("pediatric-hospitals", "소아청소년과 병원", 216, true),
    FACILITY_GEOCODING("facility-geocoding", "시설 좌표 보정", 36, false),
    POLICY_CHANGE_NOTICE("policy-change-notice", "정책 변경 알림", 36, false),
    FACILITY_VACANCY_NOTICE("facility-vacancy-notice", "빈자리 알림", 36, false),
    POLICY_DEADLINE_NOTICE("policy-deadline-notice", "마감 임박 알림", 36, false),
    BENEFIT_REPORT_SOLICIT("benefit-report-solicit", "실수령액 제보 요청", 192, false);

    private final String code;
    private final String label;
    private final int staleAfterHours;

    /** 공개 데이터의 신선도를 결정하는 작업인가. 알림 작업은 데이터를 갱신하지 않는다. */
    private final boolean dataFreshness;

    SyncJob(String code, String label, int staleAfterHours, boolean dataFreshness) {
        this.code = code;
        this.label = label;
        this.staleAfterHours = staleAfterHours;
        this.dataFreshness = dataFreshness;
    }

    public static SyncJob ofCode(String code) {
        for (SyncJob job : values()) {
            if (job.code.equals(code)) {
                return job;
            }
        }
        throw new IllegalArgumentException("알 수 없는 작업 코드입니다: " + code);
    }
}
