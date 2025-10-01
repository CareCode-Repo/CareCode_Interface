package com.carecode.domain.careFacility.entity;

/**
 * 육아 시설 유형 열거형
 */
public enum FacilityType {
    KINDERGARTEN("유치원"),
    DAYCARE("어린이집"),
    PLAYGROUP("놀이방"),
    NURSERY("보육원"),
    OTHER("기타");

    private final String displayName;

    FacilityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 문자열을 FacilityType으로 안전하게 변환
     */
    public static FacilityType fromString(String type) {
        try {
            return FacilityType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return OTHER;
        }
    }
}
