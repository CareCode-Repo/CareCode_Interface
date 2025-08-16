package com.carecode.core.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 로그인 실패 처리 핸들러
 */
@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.error("OAuth2 로그인 실패: {}", exception.getMessage(), exception);

        String errorMessage = "Social login failed";
        
        // 예외 타입에 따른 구체적인 메시지 설정
        if (exception.getMessage() != null) {
            if (exception.getMessage().contains("access_denied")) {
                errorMessage = "Login cancelled";
            } else if (exception.getMessage().contains("invalid_request")) {
                errorMessage = "Invalid request";
            } else if (exception.getMessage().contains("invalid_token_response")) {
                errorMessage = "Token response error";
            } else if (exception.getMessage().contains("server_error")) {
                errorMessage = "Server error";
            }
        }

        // JSON 오류 응답 직접 전송 (리다이렉트 없이)
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        String jsonResponse = String.format("""
            {
                "success": false,
                "error": "%s",
                "errorType": "oauth2_failure",
                "message": "OAuth2 authentication failed"
            }
            """, errorMessage);
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}