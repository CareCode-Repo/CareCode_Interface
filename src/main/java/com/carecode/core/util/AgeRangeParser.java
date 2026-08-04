package com.carecode.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 공공데이터의 자유 텍스트 지원대상에서 연령 조건을 개월 단위로 뽑아낸다. */
public final class AgeRangeParser {

    /** 정책 연령 필드의 단위는 개월이다. 세 단위 표기는 모두 여기서 개월로 환산한다. */
    private static final int MONTHS_PER_YEAR = 12;

    /** 육아 정책의 상한. 이보다 큰 숫자는 연령이 아니라 금액·연도일 가능성이 높다. */
    private static final int MAX_PLAUSIBLE_YEAR = 19;
    private static final int MAX_PLAUSIBLE_MONTH = MAX_PLAUSIBLE_YEAR * MONTHS_PER_YEAR;

    // "생후 12~23개월", "0~23 개월"
    private static final Pattern MONTH_RANGE = Pattern.compile("(\\d{1,3})\\s*[~\\-–]\\s*(\\d{1,3})\\s*개월");
    // "24개월 미만", "36개월 이하", "12개월 이상"
    private static final Pattern MONTH_BOUND = Pattern.compile("(\\d{1,3})\\s*개월\\s*(미만|이하|이상)");
    // "만 3~5세", "만 3세 ~ 만 5세", "3세~5세"
    private static final Pattern YEAR_RANGE =
            Pattern.compile("(?:만\\s*)?(\\d{1,2})\\s*세?\\s*[~\\-–]\\s*(?:만\\s*)?(\\d{1,2})\\s*세");
    // "만 7세 미만", "만 8세 이하", "만 6세 이상"
    private static final Pattern YEAR_BOUND = Pattern.compile("(?:만\\s*)?(\\d{1,2})\\s*세\\s*(미만|이하|이상)");

    private AgeRangeParser() {
    }

    /** 연령 조건. 경계가 없으면 해당 값이 null 이다. */
    public record AgeRange(Integer minMonths, Integer maxMonths) {
    }

    /** 연령 조건을 찾지 못하면 null 을 반환한다. 억지로 추정하지 않는다. */
    public static AgeRange parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        // 개월 표기가 더 정확하므로 먼저 본다.
        AgeRange range = matchMonthRange(text);
        if (range != null) {
            return range;
        }
        range = matchYearRange(text);
        if (range != null) {
            return range;
        }
        range = matchMonthBound(text);
        if (range != null) {
            return range;
        }
        return matchYearBound(text);
    }

    private static AgeRange matchMonthRange(String text) {
        Matcher m = MONTH_RANGE.matcher(text);
        if (!m.find()) {
            return null;
        }
        int min = Integer.parseInt(m.group(1));
        int max = Integer.parseInt(m.group(2));
        return isValid(min, max, MAX_PLAUSIBLE_MONTH) ? new AgeRange(min, max) : null;
    }

    private static AgeRange matchYearRange(String text) {
        Matcher m = YEAR_RANGE.matcher(text);
        if (!m.find()) {
            return null;
        }
        int minYear = Integer.parseInt(m.group(1));
        int maxYear = Integer.parseInt(m.group(2));
        if (!isValid(minYear, maxYear, MAX_PLAUSIBLE_YEAR)) {
            return null;
        }
        // "만 3~5세" 는 5세 생일부터 6세 생일 전날까지 포함하므로 상한은 71개월이다.
        return new AgeRange(minYear * MONTHS_PER_YEAR, maxYear * MONTHS_PER_YEAR + MONTHS_PER_YEAR - 1);
    }

    private static AgeRange matchMonthBound(String text) {
        Matcher m = MONTH_BOUND.matcher(text);
        if (!m.find()) {
            return null;
        }
        int months = Integer.parseInt(m.group(1));
        if (months > MAX_PLAUSIBLE_MONTH) {
            return null;
        }
        return switch (m.group(2)) {
            case "미만" -> new AgeRange(0, months - 1);
            case "이하" -> new AgeRange(0, months);
            default -> new AgeRange(months, null);
        };
    }

    private static AgeRange matchYearBound(String text) {
        Matcher m = YEAR_BOUND.matcher(text);
        if (!m.find()) {
            return null;
        }
        int years = Integer.parseInt(m.group(1));
        if (years > MAX_PLAUSIBLE_YEAR) {
            return null;
        }
        return switch (m.group(2)) {
            case "미만" -> new AgeRange(0, years * MONTHS_PER_YEAR - 1);
            case "이하" -> new AgeRange(0, years * MONTHS_PER_YEAR + MONTHS_PER_YEAR - 1);
            default -> new AgeRange(years * MONTHS_PER_YEAR, null);
        };
    }

    private static boolean isValid(int min, int max, int upperBound) {
        return min <= max && max <= upperBound;
    }
}
