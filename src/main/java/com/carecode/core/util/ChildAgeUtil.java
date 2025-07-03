package com.carecode.core.util;

import java.time.LocalDate;
import java.time.Period;

/**
 * 자녀 연령 관련 유틸리티 클래스
 * 육아 서비스에서 연령별 맞춤 정보 제공에 활용
 */
public class ChildAgeUtil {

    /**
     * 생년월일로부터 만 나이 계산
     */
    public static int calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * 생년월일로부터 개월 수 계산
     */
    public static int calculateMonths(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        Period period = Period.between(birthDate, LocalDate.now());
        return period.getYears() * 12 + period.getMonths();
    }

    /**
     * 연령대 분류
     */
    public static String getAgeGroup(int age) {
        if (age < 1) {
            return "영아";
        } else if (age < 3) {
            return "유아";
        } else if (age < 6) {
            return "미취학";
        } else if (age < 12) {
            return "초등학생";
        } else {
            return "청소년";
        }
    }

    /**
     * 개월 수로 연령대 분류
     */
    public static String getAgeGroupByMonths(int months) {
        if (months < 12) {
            return "영아";
        } else if (months < 36) {
            return "유아";
        } else if (months < 72) {
            return "미취학";
        } else if (months < 144) {
            return "초등학생";
        } else {
            return "청소년";
        }
    }

    /**
     * 육아 정책 대상 연령인지 확인
     */
    public static boolean isPolicyEligible(int age) {
        return age >= 0 && age <= 12;
    }

    /**
     * 어린이집 대상 연령인지 확인
     */
    public static boolean isDaycareEligible(int age) {
        return age >= 0 && age <= 5;
    }

    /**
     * 초등학교 대상 연령인지 확인
     */
    public static boolean isElementaryEligible(int age) {
        return age >= 6 && age <= 12;
    }
} 