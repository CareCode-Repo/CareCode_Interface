package com.carecode.core.util;

public class CommonUtil {

    /**
     * Object를 Double로 안전하게 파싱
     *
     * @param value 파싱할 값
     * @return 파싱된 Double 값, 실패 시 null
     */
    public static Double parseDouble(Object value) {
        if (value == null || value.toString().trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            System.err.println("Double 파싱 실패: " + value);
            return null;
        }
    }

    /**
     * Object를 Integer로 안전하게 파싱
     *
     * @param value 파싱할 값
     * @return 파싱된 Integer 값, 실패 시 null
     */
    public static Integer parseInteger(Object value) {
        if (value == null || value.toString().trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            System.err.println("Integer 파싱 실패: " + value);
            return null;
        }
    }
}
