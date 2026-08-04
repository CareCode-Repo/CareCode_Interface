package com.carecode.core.client.constants;

import java.util.Map;

/** 공공데이터 API 공통 상수. */
public final class PublicDataApiConstants {

    /** 공공데이터포털 공통 응답 코드. */
    public static final String RESULT_CODE_SUCCESS = "00";

    /** 한 번에 요청할 수 있는 최대 건수 (서울 열린데이터광장 기준). */
    public static final int MAX_NUM_OF_ROWS = 1000;

    /** 시도명 → 법정동 시도 코드. */
    public static final Map<String, String> SIDO_CODES = Map.ofEntries(
            Map.entry("서울특별시", "11"),
            Map.entry("부산광역시", "21"),
            Map.entry("대구광역시", "22"),
            Map.entry("인천광역시", "23"),
            Map.entry("광주광역시", "24"),
            Map.entry("대전광역시", "25"),
            Map.entry("울산광역시", "26"),
            Map.entry("세종특별자치시", "29"),
            Map.entry("경기도", "31"),
            Map.entry("강원특별자치도", "32"),
            Map.entry("충청북도", "33"),
            Map.entry("충청남도", "34"),
            Map.entry("전북특별자치도", "35"),
            Map.entry("전라남도", "36"),
            Map.entry("경상북도", "37"),
            Map.entry("경상남도", "38"),
            Map.entry("제주특별자치도", "39"));

    private PublicDataApiConstants() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /** 시도명으로 시도 코드를 찾는다. */
    public static String findSidoCode(String sidoName) {
        if (sidoName == null || sidoName.isBlank()) {
            return null;
        }
        String normalized = sidoName.trim();

        String exact = SIDO_CODES.get(normalized);
        if (exact != null) {
            return exact;
        }
        return SIDO_CODES.entrySet().stream()
                .filter(e -> e.getKey().startsWith(normalized) || normalized.startsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
