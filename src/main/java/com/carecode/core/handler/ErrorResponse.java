package com.carecode.core.handler;

import com.carecode.core.exception.ErrorCode;
import com.carecode.core.util.LoggingUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 표준화된 에러 응답 DTO */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    private String code;
    private String message;
    private String details;
    private LocalDateTime timestamp;

    /**
     * 이 요청의 추적 ID. 응답 헤더 {@code X-Request-Id} 와 같은 값이다.
     *
     * <p>본문에도 담는 이유는 사용자가 오류 화면을 캡처해 보내는 경우가 대부분이기 때문이다.
     * 헤더는 캡처에 안 나온다.
     */
    private String traceId;
    
    public static ErrorResponse of(ErrorCode errorCode, String details) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .details(details)
                .timestamp(LocalDateTime.now())
                .traceId(LoggingUtil.getTraceId())
                .build();
    }
    
    public static ErrorResponse of(ErrorCode errorCode, String customMessage, String details) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(customMessage)
                .details(details)
                .timestamp(LocalDateTime.now())
                .traceId(LoggingUtil.getTraceId())
                .build();
    }
    
    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .timestamp(LocalDateTime.now())
                .traceId(LoggingUtil.getTraceId())
                .build();
    }
}

