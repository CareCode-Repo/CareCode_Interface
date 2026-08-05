package com.carecode.domain.health.entity;

import java.util.Arrays;
import java.util.List;

/** 국가예방접종(NIP) 표준 일정. 각 백신의 회차별 접종 시기를 생후 개월 수로 정의한다. */
public enum VaccineType {

    BCG("BCG(결핵)", List.of(0)),
    HEP_B("B형간염", List.of(0, 1, 6)),
    DTAP("DTaP(디프테리아·파상풍·백일해)", List.of(2, 4, 6, 15, 48)),
    IPV("폴리오", List.of(2, 4, 6, 48)),
    HIB("Hib(b형헤모필루스인플루엔자)", List.of(2, 4, 6, 12)),
    PCV("폐렴구균", List.of(2, 4, 6, 12)),
    ROTAVIRUS("로타바이러스", List.of(2, 4, 6)),
    MMR("MMR(홍역·유행성이하선염·풍진)", List.of(12, 48)),
    VARICELLA("수두", List.of(12)),
    HEP_A("A형간염", List.of(12, 18)),
    JAPANESE_ENCEPHALITIS("일본뇌염", List.of(12, 13, 24, 72)),
    INFLUENZA("인플루엔자", List.of(6));

    private final String displayName;

    /** 회차별 접종 시기(생후 개월 수). 인덱스 0 이 1차. */
    private final List<Integer> doseMonths;

    VaccineType(String displayName, List<Integer> doseMonths) {
        this.displayName = displayName;
        this.doseMonths = doseMonths;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<Integer> getDoseMonths() {
        return doseMonths;
    }

    public int getTotalDoses() {
        return doseMonths.size();
    }

    public int getMonthsForDose(int doseNumber) {
        if (doseNumber < 1 || doseNumber > doseMonths.size()) {
            throw new IllegalArgumentException(
                    name() + " 백신에 존재하지 않는 회차입니다: " + doseNumber);
        }
        return doseMonths.get(doseNumber - 1);
    }

    public static List<VaccineType> all() {
        return Arrays.asList(values());
    }
}
