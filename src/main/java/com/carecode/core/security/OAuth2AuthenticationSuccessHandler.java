package com.carecode.core.security;

import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import com.carecode.domain.user.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * OAuth2 로그인 성공 처리 핸들러
 * 카카오 로그인 성공 시 JWT 토큰을 발급하고 프론트엔드로 리다이렉트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 로그인 성공 처리 시작");

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = oAuth2User.getAttributes();

            // 사용자 정보 추출 (닉네임 기반)
            String userName = extractNameFromAttributes(attributes);
            String kakaoId = extractKakaoIdFromAttributes(attributes);
            if (userName == null || kakaoId == null) {
                log.error("OAuth2 사용자로부터 닉네임 또는 ID를 추출할 수 없습니다.");
                handleError(response, "User info unavailable");
                return;
            }

            // 고유한 사용자명 생성
            String uniqueUserName = userName + "_" + kakaoId;

            // 사용자 정보 조회/업데이트 (고유한 사용자명 기반)
            User user = userRepository.findByName(uniqueUserName)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + uniqueUserName));

            // 마지막 로그인 시간 업데이트
            user.setLastLoginAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // JWT 토큰 생성
            String accessToken = jwtService.generateAccessToken(
                    user.getUserId(),
                    user.getEmail(),
                    user.getRole().name()
            );
            String refreshToken = jwtService.generateRefreshToken(
                    user.getUserId(),
                    user.getEmail()
            );

            log.info("OAuth2 로그인 성공: 사용자={}, 토큰 발급 완료", uniqueUserName);

            // JSON 응답 직접 전송 (리다이렉트 없이)
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            
            String jsonResponse = String.format("""
                {
                    "success": true,
                    "message": "Login successful",
                    "accessToken": "%s",
                    "refreshToken": "%s",
                    "tokenType": "Bearer",
                    "expiresIn": 3600000,
                    "user": {
                        "userId": "%s",
                        "email": "%s",
                        "role": "%s",
                        "name": "%s"
                    }
                }
                """, accessToken, refreshToken, user.getUserId(), user.getEmail(), 
                user.getRole().name(), user.getName());
            
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();

        } catch (RuntimeException e) {
            if (e.getMessage().contains("사용자를 찾을 수 없습니다")) {
                log.info("신규 사용자 OAuth2 로그인 시도: {}", e.getMessage());
                handleNewUserError(response, "신규 사용자입니다. 회원가입이 필요합니다.");
            } else {
                log.error("OAuth2 로그인 성공 처리 중 오류 발생: {}", e.getMessage(), e);
                handleError(response, "Login processing error");
            }
        } catch (Exception e) {
            log.error("OAuth2 로그인 성공 처리 중 오류 발생: {}", e.getMessage(), e);
            handleError(response, "Login processing error");
        }
    }

    /**
     * OAuth2 사용자 속성에서 닉네임 추출
     */
    private String extractNameFromAttributes(Map<String, Object> attributes) {
        // 카카오의 경우
        if (attributes.containsKey("kakao_account")) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            if (kakaoAccount != null && kakaoAccount.containsKey("profile")) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null && profile.containsKey("nickname")) {
                    return (String) profile.get("nickname");
                }
            }
        }

        // 구글의 경우
        if (attributes.containsKey("name")) {
            return (String) attributes.get("name");
        }

        return null;
    }

    /**
     * OAuth2 사용자 속성에서 카카오 ID 추출
     */
    private String extractKakaoIdFromAttributes(Map<String, Object> attributes) {
        if (attributes.containsKey("id")) {
            Object id = attributes.get("id");
            return id != null ? id.toString() : null;
        }
        return null;
    }

    /**
     * 신규 사용자 오류 처리
     */
    private void handleNewUserError(HttpServletResponse response, String errorMessage) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        
        String jsonResponse = String.format("""
            {
                "success": false,
                "error": "USER_NOT_FOUND",
                "message": "%s",
                "redirectToRegistration": true
            }
            """, errorMessage);
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    /**
     * 오류 처리
     */
    private void handleError(HttpServletResponse response, String errorMessage) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        
        String jsonResponse = String.format("""
            {
                "success": false,
                "error": "%s",
                "message": "OAuth2 login processing failed"
            }
            """, errorMessage);
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}