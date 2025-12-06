package com.carecode.core.util;

import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 로깅 유틸리티
 * MDC를 활용한 구조화된 로깅 지원
 */
public class LoggingUtil {
    
    private static final String TRACE_ID_KEY = "traceId";
    private static final String USER_ID_KEY = "userId";
    private static final String CHILD_ID_KEY = "childId";
    private static final String REQUEST_ID_KEY = "requestId";
    
    /**
     * 트레이스 ID 설정 (요청 추적용)
     */
    public static void setTraceId(String traceId) {
        if (StringUtils.hasText(traceId)) {
            MDC.put(TRACE_ID_KEY, traceId);
        } else {
            MDC.put(TRACE_ID_KEY, generateTraceId());
        }
    }
    
    /**
     * 새로운 트레이스 ID 생성
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 사용자 ID 설정
     */
    public static void setUserId(String userId) {
        if (StringUtils.hasText(userId)) {
            MDC.put(USER_ID_KEY, userId);
        }
    }
    
    /**
     * 아동 ID 설정
     */
    public static void setChildId(String childId) {
        if (StringUtils.hasText(childId)) {
            MDC.put(CHILD_ID_KEY, childId);
        }
    }
    
    /**
     * 요청 ID 설정
     */
    public static void setRequestId(String requestId) {
        if (StringUtils.hasText(requestId)) {
            MDC.put(REQUEST_ID_KEY, requestId);
        }
    }
    
    /**
     * 모든 MDC 컨텍스트 초기화
     */
    public static void clear() {
        MDC.clear();
    }
    
    /**
     * 현재 트레이스 ID 조회
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }
    
    /**
     * 현재 사용자 ID 조회
     */
    public static String getUserId() {
        return MDC.get(USER_ID_KEY);
    }
}

