package com.carecode.core.security;

import com.carecode.domain.user.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/** JWT 인증 필터. 요청에서 JWT 토큰을 추출하고 검증하여 인증 정보를 설정한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.debug("JWT 필터 실행: {}", request.getRequestURI());

        try {
            authenticateIfTokenPresent(request);
        } catch (Exception e) {
            // 토큰 처리 중 예상치 못한 예외는 인증 실패로 취급한다.
            log.error("JWT 인증 필터 오류: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에 토큰이 있을 때만 인증을 시도한다.
     *
     * <p>토큰이 아예 없으면 아무것도 건드리지 않는다. 예전에는 이 경우에도
     * {@code SecurityContextHolder.clearContext()} 를 호출했다. 운영에서는 JWT 외에
     * 인증 수단이 없어 결과가 같았지만, 자기가 세우지 않은 컨텍스트를 지우는 필터라
     * 앞단에서 인증을 넣어주는 경로를 전부 조용히 무력화한다
     * (테스트의 {@code @WithMockUser}, 나중에 세션·OAuth2 를 병행할 때의 인증 등).
     * 실제로 접근제어 테스트가 이유 없이 401 로 떨어져 드러난 문제다.
     */
    private void authenticateIfTokenPresent(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);

        if (!StringUtils.hasText(token)) {
            log.debug("JWT 토큰 없음 - 기존 인증 정보를 유지한 채 통과시킵니다");
            return;
        }

        // Access Token 만 허용한다. Refresh Token 으로는 API 인증이 되지 않아야 한다.
        if (!jwtService.validateAccessToken(token)) {
            log.debug("JWT 토큰이 유효하지 않음");
            SecurityContextHolder.clearContext();
            return;
        }

        String userId = jwtService.getUserIdFromToken(token);
        String email = jwtService.getEmailFromToken(token);
        String role = jwtService.getRoleFromToken(token);

        if (!StringUtils.hasText(email) || !StringUtils.hasText(role)) {
            // role 이 없으면 "ROLE_null" 권한으로 인증되던 문제를 차단한다.
            log.warn("JWT 인증 거부: 필수 클레임 누락 (email={}, role={})", email, role);
            SecurityContextHolder.clearContext();
            return;
        }

        // principal 은 email 로 통일한다 (CurrentUserFacade 가 email 로 사용자를 찾는다).
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                email,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("JWT 인증 성공: userId={}, email={}, role={}", userId, email, role);
    }

    // 요청에서 JWT 토큰 추출
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // 토큰을 볼 필요가 없는 공개 경로. 여기서 빠져도 인가는 SecurityConfig 가 담당한다.
        return path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/actuator") ||
               path.equals("/") ||
               path.equals("/error") ||
               path.equals("/favicon.ico") ||
               path.startsWith("/auth/login") ||
               path.startsWith("/auth/register") ||
               path.equals("/auth/refresh") ||
               path.startsWith("/auth/kakao") ||
               path.startsWith("/oauth2") ||
               path.startsWith("/login/oauth2") ||
               path.equals("/kakao-callback.html");
    }
}
