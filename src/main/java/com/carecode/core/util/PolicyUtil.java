package com.carecode.core.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 육아 정책 관련 유틸리티 클래스
 * 정책 정보 처리 및 검증에 활용
 */
public class PolicyUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 정책 신청 기간이 유효한지 확인
     */
    public static boolean isApplicationPeriodValid(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    /**
     * 정책 신청 기간이 남았는지 확인
     */
    public static boolean isApplicationPeriodRemaining(LocalDate endDate) {
        if (endDate == null) {
            return false;
        }
        
        return LocalDate.now().isBefore(endDate);
    }

    /**
     * 정책 신청 기간까지 남은 일수 계산
     */
    public static long getRemainingDays(LocalDate endDate) {
        if (endDate == null) {
            return 0;
        }
        
        LocalDate today = LocalDate.now();
        if (today.isAfter(endDate)) {
            return 0;
        }
        
        return java.time.temporal.ChronoUnit.DAYS.between(today, endDate);
    }

    /**
     * 정책 유형 분류
     */
    public static String getPolicyType(String policyCode) {
        if (policyCode == null) {
            return "기타";
        }
        
        if (policyCode.startsWith("DAYCARE")) {
            return "어린이집";
        } else if (policyCode.startsWith("KINDERGARTEN")) {
            return "유치원";
        } else if (policyCode.startsWith("VOUCHER")) {
            return "바우처";
        } else if (policyCode.startsWith("SUBSIDY")) {
            return "보조금";
        } else if (policyCode.startsWith("MEDICAL")) {
            return "의료";
        } else {
            return "기타";
        }
    }

    /**
     * 정책 우선순위 계산
     */
    public static int calculatePriority(String policyType, int childAge, String region) {
        int priority = 0;
        
        // 연령별 우선순위
        if (childAge < 1) {
            priority += 10; // 영아 우선
        } else if (childAge < 3) {
            priority += 8;  // 유아
        } else if (childAge < 6) {
            priority += 6;  // 미취학
        }
        
        // 정책 유형별 우선순위
        switch (policyType) {
            case "어린이집":
                priority += 5;
                break;
            case "바우처":
                priority += 4;
                break;
            case "보조금":
                priority += 3;
                break;
            case "의료":
                priority += 2;
                break;
        }
        
        return priority;
    }

    /**
     * 정책 상태 확인
     */
    public static String getPolicyStatus(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        
        if (today.isBefore(startDate)) {
            return "신청 예정";
        } else if (today.isAfter(endDate)) {
            return "신청 마감";
        } else {
            return "신청 중";
        }
    }
} 