package com.carecode.core.controller;

/**
 * 컨트롤러 공통 상수.
 *
 * <p>예전에는 여기에 {@code @RequestMapping("/api/v1")} 이 있었지만, 하위 컨트롤러가 전부 자기
 * {@code @RequestMapping} 으로 덮어써서 어떤 경로에도 적용된 적이 없다. "경로 버전을 쓰고 있다" 는
 * 오해만 낳아 지웠다. API 버전은 {@link com.carecode.core.web.ApiVersionFilter} 가 헤더로 다룬다.
 */
public abstract class BaseController {
    
    // 공통 응답 메시지 상수
    protected static final String SUCCESS_MESSAGE = "요청이 성공적으로 처리되었습니다.";
    protected static final String NOT_FOUND_MESSAGE = "요청한 리소스를 찾을 수 없습니다.";
    protected static final String UNAUTHORIZED_MESSAGE = "인증이 필요합니다.";
    protected static final String FORBIDDEN_MESSAGE = "접근 권한이 없습니다.";
    protected static final String VALIDATION_ERROR_MESSAGE = "입력 데이터가 올바르지 않습니다.";
    
    // 공통 응답 코드
    protected static final String SUCCESS_CODE = "SUCCESS";
    protected static final String ERROR_CODE = "ERROR";
    protected static final String NOT_FOUND_CODE = "NOT_FOUND";
    protected static final String UNAUTHORIZED_CODE = "UNAUTHORIZED";
    protected static final String FORBIDDEN_CODE = "FORBIDDEN";
    protected static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";
} 