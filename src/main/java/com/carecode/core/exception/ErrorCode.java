package com.carecode.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 * 도메인별로 그룹화하여 관리
 */
@Getter
public enum ErrorCode {
    
    // ===== 공통 에러 (C000) =====
    INTERNAL_SERVER_ERROR("C000", "서버 내부 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT("C001", "입력값이 유효하지 않습니다", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("C002", "인증이 필요합니다", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("C003", "접근 권한이 없습니다", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("C004", "요청한 리소스를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    
    // ===== 사용자 관련 에러 (U000) =====
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("U002", "이미 존재하는 사용자입니다", HttpStatus.CONFLICT),
    USER_INACTIVE("U003", "비활성화된 사용자입니다", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS("U004", "이메일 또는 비밀번호가 올바르지 않습니다", HttpStatus.UNAUTHORIZED),
    EMAIL_ALREADY_VERIFIED("U005", "이미 인증된 이메일입니다", HttpStatus.BAD_REQUEST),
    EMAIL_VERIFICATION_EXPIRED("U006", "이메일 인증 링크가 만료되었습니다", HttpStatus.BAD_REQUEST),
    
    // ===== 건강 관리 관련 에러 (H000) =====
    HEALTH_RECORD_NOT_FOUND("H001", "건강 기록을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    CHILD_NOT_FOUND("H002", "아동을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    INVALID_CHILD_ID("H003", "유효하지 않은 아동 ID입니다", HttpStatus.BAD_REQUEST),
    INVALID_RECORD_ID("H004", "유효하지 않은 기록 ID입니다", HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE("H005", "시작일은 종료일보다 이전이어야 합니다", HttpStatus.BAD_REQUEST),
    INVALID_MONTHS("H006", "개월 수는 1 이상 120 이하여야 합니다", HttpStatus.BAD_REQUEST),
    INVALID_CHART_TYPE("H007", "지원하지 않는 차트 타입입니다", HttpStatus.BAD_REQUEST),
    HOSPITAL_NOT_FOUND("H008", "병원을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    HOSPITAL_REVIEW_NOT_FOUND("H009", "병원 리뷰를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    HOSPITAL_REVIEW_ACCESS_DENIED("H010", "리뷰를 수정/삭제할 권한이 없습니다", HttpStatus.FORBIDDEN),
    
    // ===== 돌봄 시설 관련 에러 (F000) =====
    CARE_FACILITY_NOT_FOUND("F001", "돌봄 시설을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    BOOKING_NOT_FOUND("F002", "예약을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    BOOKING_ALREADY_EXISTS("F003", "이미 예약된 시간입니다", HttpStatus.CONFLICT),
    BOOKING_ACCESS_DENIED("F004", "예약에 대한 권한이 없습니다", HttpStatus.FORBIDDEN),
    
    // ===== 정책 관련 에러 (P000) =====
    POLICY_NOT_FOUND("P001", "정책을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    POLICY_CATEGORY_NOT_FOUND("P002", "정책 카테고리를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    
    // ===== 커뮤니티 관련 에러 (M000) =====
    POST_NOT_FOUND("M001", "게시글을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    POST_ACCESS_DENIED("M002", "게시글에 대한 권한이 없습니다", HttpStatus.FORBIDDEN),
    COMMENT_NOT_FOUND("M003", "댓글을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    COMMENT_ACCESS_DENIED("M004", "댓글에 대한 권한이 없습니다", HttpStatus.FORBIDDEN),
    
    // ===== 알림 관련 에러 (N000) =====
    NOTIFICATION_NOT_FOUND("N001", "알림을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    NOTIFICATION_TEMPLATE_NOT_FOUND("N002", "알림 템플릿을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    
    // ===== 챗봇 관련 에러 (B000) =====
    CHAT_SESSION_NOT_FOUND("B001", "채팅 세션을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    CHAT_MESSAGE_NOT_FOUND("B002", "채팅 메시지를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    
    // ===== 외부 API 관련 에러 (E000) =====
    EXTERNAL_API_ERROR("E001", "외부 API 호출 중 오류가 발생했습니다", HttpStatus.BAD_GATEWAY),
    EXTERNAL_API_TIMEOUT("E002", "외부 API 호출 시간이 초과되었습니다", HttpStatus.GATEWAY_TIMEOUT);
    
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    
    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}

