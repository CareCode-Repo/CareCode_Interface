package com.carecode.domain.health.timeline;

import lombok.Getter;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 국가 영유아 건강검진 시기(월령 구간).
 *
 * <p>이 앱에는 표준 검진 시기가 없었다. {@code getCheckupSchedule} 은 이름과 달리 이미 기록된 검진을
 * 나열할 뿐이어서, 아직 받지 않은 검진은 화면에 나타나지 않았다 — 놓쳐도 아무도 알려주지 않는다.
 * 예방접종 일정({@code VaccineType})과 같은 방식으로 시기를 코드에 담는다.
 *
 * <p>출처: 국민건강보험 영유아 건강검진(생후 14일~71개월, 8차). 구강검진은 별도 회차라 포함하지 않는다.
 * 실제 대상 기간은 제도 개편으로 바뀔 수 있으므로 화면에는 "권장 시기" 로 표시한다.
 */
@Getter
public enum CheckupStandard {

    ROUND_1(1, 14, 35, "1차 건강검진", "생후 14~35일"),
    ROUND_2(2, 4 * 30, 6 * 30 + 30, "2차 건강검진", "4~6개월"),
    ROUND_3(3, 9 * 30, 12 * 30 + 30, "3차 건강검진", "9~12개월"),
    ROUND_4(4, 18 * 30, 24 * 30 + 30, "4차 건강검진", "18~24개월"),
    ROUND_5(5, 30 * 30, 36 * 30 + 30, "5차 건강검진", "30~36개월"),
    ROUND_6(6, 42 * 30, 48 * 30 + 30, "6차 건강검진", "42~48개월"),
    ROUND_7(7, 54 * 30, 60 * 30 + 30, "7차 건강검진", "54~60개월"),
    ROUND_8(8, 66 * 30, 71 * 30 + 30, "8차 건강검진", "66~71개월");

    private final int round;

    /** 생후 일수 기준 시작·종료. 월령 구간을 일수로 환산해 둔다(월 길이 차이는 안내 문구로 흡수). */
    private final int startDays;
    private final int endDays;

    private final String title;
    private final String periodLabel;

    CheckupStandard(int round, int startDays, int endDays, String title, String periodLabel) {
        this.round = round;
        this.startDays = startDays;
        this.endDays = endDays;
        this.title = title;
        this.periodLabel = periodLabel;
    }

    public LocalDate windowStart(LocalDate birthDate) {
        return birthDate.plusDays(startDays);
    }

    public LocalDate windowEnd(LocalDate birthDate) {
        return birthDate.plusDays(endDays);
    }

    public static List<CheckupStandard> all() {
        return Arrays.asList(values());
    }
}
