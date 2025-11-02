package com.carecode.core.util;

/**
 * 공통 요청 매퍼 인터페이스
 */
public interface RequestMapper<RQ, E> {
    E toEntity(RQ request);
}


