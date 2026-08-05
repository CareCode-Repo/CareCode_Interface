package com.carecode.core.handler;

import com.carecode.core.exception.*;
import com.carecode.core.ops.OperationalAlerter;
import com.carecode.domain.user.service.ConsentGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** 전역 예외 핸들러 모든 예외를 일관된 형식으로 처리 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class CustomizedResponseEntityExceptionHandler {

    private final OperationalAlerter alerter;

    // CareCodeException 계층의 예외 처리
    @ExceptionHandler(CareCodeException.class)
    public ResponseEntity<ErrorResponse> handleCareCodeException(CareCodeException ex, WebRequest request) {
        log.warn("CareCodeException 발생: {} - {}", ex.getErrorCode().getCode(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(errorResponse);
    }

    // UserNotFoundException 처리 (하위 호환성 유지)
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex, WebRequest request) {
        log.warn("UserNotFoundException 발생: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.USER_NOT_FOUND,
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    // ResourceNotFoundException 처리 (하위 호환성 유지)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.warn("ResourceNotFoundException 발생: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.RESOURCE_NOT_FOUND,
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    // BusinessException 처리 (하위 호환성 유지)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("BusinessException 발생: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.INVALID_INPUT,
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /** 동의가 없어 막힌 경우. 클라이언트가 어떤 동의를 받아야 하는지 알아야 화면을 띄울 수 있다. */
    @ExceptionHandler(ConsentGuard.ConsentRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleConsentRequired(
            ConsentGuard.ConsentRequiredException ex, WebRequest request) {
        log.info("동의 미완료로 접근 차단: {}", ex.getConsentType());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "CONSENT_REQUIRED");
        body.put("consentType", ex.getConsentType().name());
        body.put("displayName", ex.getConsentType().getDisplayName());
        body.put("sensitive", ex.getConsentType().isSensitive());
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // CareServiceException 처리 (하위 호환성 유지)
    @ExceptionHandler(CareServiceException.class)
    public ResponseEntity<ErrorResponse> handleCareServiceException(CareServiceException ex, WebRequest request) {
        log.error("CareServiceException 발생: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);

        ErrorCode errorCode = resolveCareServiceErrorCode(ex.getErrorCode());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            errorCode,
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(errorResponse);
    }

    private ErrorCode resolveCareServiceErrorCode(String careServiceErrorCode) {
        if ("UNAUTHORIZED".equalsIgnoreCase(careServiceErrorCode)) {
            return ErrorCode.UNAUTHORIZED;
        }
        if ("FORBIDDEN".equalsIgnoreCase(careServiceErrorCode)) {
            return ErrorCode.FORBIDDEN;
        }
        if ("USER_NOT_FOUND".equalsIgnoreCase(careServiceErrorCode)) {
            return ErrorCode.USER_NOT_FOUND;
        }
        if (careServiceErrorCode != null && careServiceErrorCode.startsWith("KAKAO")) {
            return ErrorCode.INVALID_INPUT;
        }
        return ErrorCode.INTERNAL_SERVER_ERROR;
    }

    // PolicyNotFoundException 처리 (하위 호환성 유지)
    @ExceptionHandler(PolicyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePolicyNotFoundException(PolicyNotFoundException ex, WebRequest request) {
        log.warn("PolicyNotFoundException 발생: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.POLICY_NOT_FOUND,
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    // CareFacilityNotFoundException 처리 (하위 호환성 유지)
    @ExceptionHandler(CareFacilityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCareFacilityNotFoundException(CareFacilityNotFoundException ex, WebRequest request) {
        log.warn("CareFacilityNotFoundException 발생: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.CARE_FACILITY_NOT_FOUND,
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    // Validation 예외 처리 (@Valid 실패)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation 실패: {}", ex.getMessage());
        
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    FieldError::getField,
                    error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "유효하지 않은 값입니다",
                    (existing, replacement) -> existing
                ));
        
        String details = errors.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.INVALID_INPUT,
            "입력값 검증에 실패했습니다",
            details
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    // IllegalArgumentException 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("IllegalArgumentException 발생: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.INVALID_INPUT,
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    // 모든 예외 처리 (최후의 수단)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {
        log.error("예상치 못한 예외 발생", ex);
        // 5xx 는 사용자가 이미 실패를 겪은 뒤다. 로그만 남기면 아무도 모른 채 지나간다.
        alerter.alert("unhandled-" + ex.getClass().getSimpleName(),
                "처리되지 않은 예외: " + ex.getClass().getSimpleName(),
                ex.getMessage() + System.lineSeparator() + request.getDescription(false));
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.INTERNAL_SERVER_ERROR,
            "서버 내부 오류가 발생했습니다",
            request.getDescription(false)
        );
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}