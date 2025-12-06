package com.carecode.core.util;

import org.springframework.data.domain.Sort;

/**
 * 정렬 유틸리티 클래스
 * 동적 정렬 옵션을 생성하는 헬퍼 메서드 제공
 */
public class SortUtil {

    /**
     * 정렬 필드와 방향으로 Sort 객체 생성
     * 
     * @param sortBy 정렬 필드 (null이면 기본값 사용)
     * @param sortDirection 정렬 방향 (ASC, DESC, null이면 기본값 사용)
     * @param defaultField 기본 정렬 필드
     * @param defaultDirection 기본 정렬 방향
     * @return Sort 객체
     */
    public static Sort createSort(String sortBy, String sortDirection, String defaultField, Sort.Direction defaultDirection) {
        String field = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy : defaultField;
        Sort.Direction direction = parseDirection(sortDirection, defaultDirection);
        
        return Sort.by(direction, field);
    }

    /**
     * 여러 필드로 정렬하는 Sort 객체 생성
     * 
     * @param sortBy 정렬 필드 (쉼표로 구분된 여러 필드 가능)
     * @param sortDirection 정렬 방향
     * @param defaultFields 기본 정렬 필드 배열
     * @param defaultDirection 기본 정렬 방향
     * @return Sort 객체
     */
    public static Sort createMultiSort(String sortBy, String sortDirection, String[] defaultFields, Sort.Direction defaultDirection) {
        Sort.Direction direction = parseDirection(sortDirection, defaultDirection);
        
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            String[] fields = sortBy.split(",");
            Sort.Order[] orders = new Sort.Order[fields.length];
            for (int i = 0; i < fields.length; i++) {
                orders[i] = new Sort.Order(direction, fields[i].trim());
            }
            return Sort.by(orders);
        } else {
            Sort.Order[] orders = new Sort.Order[defaultFields.length];
            for (int i = 0; i < defaultFields.length; i++) {
                orders[i] = new Sort.Order(direction, defaultFields[i]);
            }
            return Sort.by(orders);
        }
    }

    /**
     * 정렬 방향 문자열을 Sort.Direction으로 변환
     * 
     * @param sortDirection 정렬 방향 문자열 (ASC, DESC, asc, desc)
     * @param defaultDirection 기본 정렬 방향
     * @return Sort.Direction
     */
    public static Sort.Direction parseDirection(String sortDirection, Sort.Direction defaultDirection) {
        if (sortDirection == null || sortDirection.trim().isEmpty()) {
            return defaultDirection;
        }
        
        String upper = sortDirection.toUpperCase();
        if ("ASC".equals(upper)) {
            return Sort.Direction.ASC;
        } else if ("DESC".equals(upper)) {
            return Sort.Direction.DESC;
        } else {
            return defaultDirection;
        }
    }

    /**
     * 허용된 정렬 필드인지 검증
     * 
     * @param sortBy 정렬 필드
     * @param allowedFields 허용된 필드 목록
     * @return 검증 통과 여부
     */
    public static boolean isValidSortField(String sortBy, String... allowedFields) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return true; // null은 기본값 사용을 의미하므로 허용
        }
        
        String[] fields = sortBy.split(",");
        for (String field : fields) {
            field = field.trim();
            boolean found = false;
            for (String allowed : allowedFields) {
                if (field.equals(allowed)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }
}

