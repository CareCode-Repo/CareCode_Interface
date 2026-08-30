package com.carecode.core.handler;

import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ErrorCode;
import com.carecode.core.ops.OperationalAlerter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BusinessException 이 자기 ErrorCode 의 상태 코드로 응답하는지 고정한다.
 *
 * <p>예전 핸들러는 ErrorCode 를 {@code INVALID_INPUT} 으로, 상태를 400 으로 못 박았다.
 * {@code BusinessException} 은 {@code CareCodeException} 의 하위 타입이라 자기 ErrorCode 와
 * HttpStatus 를 이미 들고 있는데 그 값을 통째로 버린 것이다.
 *
 * <p>영향이 큰 쪽은 인증이다. 프런트 인터셉터는 <b>401 에서만</b> 토큰을 갱신하고
 * 로그인으로 보낸다(`src/apis/interceptor.ts`). 세션 만료가 400 으로 나가면
 * 갱신도 재로그인도 일어나지 않고 "입력값이 유효하지 않습니다" 만 보인다.
 * 실제로 그런 경로가 9곳 있었다 — HealthService 6, NotificationService 2, JwtService 1.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("전역 예외 핸들러 - BusinessException 상태 코드")
class BusinessExceptionStatusTest {

    @Mock private OperationalAlerter alerter;

    private ResponseEntity<ErrorResponse> handle(BusinessException ex) {
        CustomizedResponseEntityExceptionHandler handler =
                new CustomizedResponseEntityExceptionHandler(alerter);
        return handler.handleBusinessException(ex, new ServletWebRequest(new MockHttpServletRequest()));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "UNAUTHORIZED,   401",
            "FORBIDDEN,      403",
            "INVALID_INPUT,  400"
    })
    @DisplayName("ErrorCode 가 정한 상태 코드로 응답한다")
    void usesErrorCodeStatus(ErrorCode errorCode, int expectedStatus) {
        ResponseEntity<ErrorResponse> response =
                handle(new BusinessException(errorCode, "메시지"));

        assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
    }

    @Test
    @DisplayName("세션 만료는 401 이어야 프런트가 토큰을 갱신한다")
    void expiredSessionIsUnauthorized() {
        // JwtService.refreshTokens 가 던지는 것과 같은 예외다.
        ResponseEntity<ErrorResponse> response =
                handle(new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."));

        assertThat(response.getStatusCode().value())
                .as("400 이면 프런트 인터셉터가 갱신도 로그인 리다이렉트도 하지 않는다")
                .isEqualTo(401);
    }

    @Test
    @DisplayName("권한 없음은 403 이어야 입력 오류와 구분된다")
    void forbiddenIsNotBadRequest() {
        // HealthService 의 "해당 건강 기록에 접근할 권한이 없습니다" 와 같은 예외다.
        ResponseEntity<ErrorResponse> response =
                handle(new BusinessException(ErrorCode.FORBIDDEN, "해당 건강 기록에 접근할 권한이 없습니다."));

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    @DisplayName("메시지 없는 생성자는 기존대로 400 이다")
    void legacyConstructorStaysBadRequest() {
        // BusinessException(String) 은 ErrorCode.INVALID_INPUT 을 쓴다. 기존 동작이 바뀌면 안 된다.
        assertThat(handle(new BusinessException("입력이 잘못됐습니다")).getStatusCode().value())
                .isEqualTo(400);
    }

    @Test
    @DisplayName("응답 본문의 코드도 ErrorCode 를 따른다")
    void bodyCarriesErrorCode() {
        ResponseEntity<ErrorResponse> response =
                handle(new BusinessException(ErrorCode.FORBIDDEN, "권한 없음"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.FORBIDDEN.getCode());
    }
}
