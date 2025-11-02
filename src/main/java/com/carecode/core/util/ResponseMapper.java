package com.carecode.core.util;

/**
 * 공통 응답 매퍼 인터페이스
 */
public interface ResponseMapper<E, R> {
    R toResponse(E entity);
}


