package com.carecode.core.util;

/** 목록 API 의 페이지 파라미터를 안전한 범위로 보정한다. */
public final class PageRequestUtil {

    /** 페이지 파라미터가 없을 때 사용할 기본 크기. */
    public static final int DEFAULT_PAGE_SIZE = 100;

    /** 한 번에 조회할 수 있는 최대 행 수. */
    public static final int MAX_PAGE_SIZE = 200;

    private PageRequestUtil() {
    }

    public static int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    public static int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
