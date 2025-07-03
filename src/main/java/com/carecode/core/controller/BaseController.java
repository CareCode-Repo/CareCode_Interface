package com.carecode.core.controller;

import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 모든 컨트롤러의 기본 클래스
 * 공통 URL 경로와 기본 설정을 제공
 */
@RequestMapping("/api/v1")
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