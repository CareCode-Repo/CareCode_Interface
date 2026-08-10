package com.carecode.domain.health.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/** 자녀 전체를 한 화면에서 본다. 다자녀 가구는 아이별로 앱을 다시 여는 게 가장 큰 불편이다. */
@Getter
@Builder
public class SiblingOverviewResponse {

    private int childCount;

    /** 다자녀 기준 충족 여부. 어린이집 입소 가점과 다자녀 정책의 조건이다. */
    private boolean multiChildHousehold;

    private List<ChildSummary> children;

    /** 자녀 수 덕분에 받을 수 있게 된 정책. */
    private List<String> multiChildBenefits;

    private List<String> notes;

    @Getter
    @Builder
    public static class ChildSummary {
        private Long childId;
        private String name;
        private LocalDate birthDate;
        private Integer ageMonths;

        /** 어린이집·유치원 반 편성 기준. */
        private String classLabel;

        /** 다가오는 접종. 아이별로 흩어져 있으면 놓치기 쉽다. */
        private String nextVaccination;
        private LocalDate nextVaccinationDate;

        /** 대기 등록해 둔 시설 수. */
        private long waitlistCount;
    }
}
