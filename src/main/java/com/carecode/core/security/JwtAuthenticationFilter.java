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

/**
 * JWT 인증 필터
 * 요청에서 JWT 토큰을 추출하고 검증하여 인증 정보를 설정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        log.debug("JWT 필터 실행: {}", requestURI);
        
        try {
            String token = extractTokenFromRequest(request);
            log.debug("추출된 토큰: {}", token != null ? "존재함" : "없음");
            
            if (StringUtils.hasText(token)) {
                log.debug("토큰 유효성 검증 시작");
                boolean isValid = jwtService.validateToken(token);
                log.debug("토큰 유효성 검증 결과: {}", isValid);
                
                if (isValid) {
                    String userId = jwtService.getUserIdFromToken(token);
                    String email = jwtService.getEmailFromToken(token);
                    String role = jwtService.getRoleFromToken(token);
                    String name = jwtService.getNameFromToken(token);
                    
                    log.info("JWT 토큰 검증 성공: userId={}, email={}, role={}, name={}", userId, email, role, name);
                    log.debug("JWT 토큰에서 추출된 정보 - userId: {}, email: {}, role: {}, name: {}", userId, email, role, name);
                    
                    // 인증 정보 생성 - name을 principal로 사용 (name이 없으면 email 사용)
                    String principal = (name != null && !name.trim().isEmpty()) ? name : email;
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,   // principal을 name으로 설정 (name이 없으면 email)
                        null,        // credentials는 null
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    
                    // SecurityContext에 인증 정보 설정
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.info("JWT 인증 성공: userId={}, email={}, role={}", userId, email, role);
                } else {
                    log.debug("JWT 토큰이 유효하지 않음");
                    SecurityContextHolder.clearContext();
                }
            } else {
                log.debug("JWT 토큰이 없음");
                SecurityContextHolder.clearContext();
            }
        } catch (Exception e) {
            log.error("JWT 인증 필터 오류: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * 요청에서 JWT 토큰 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        log.debug("Authorization 헤더: {}", bearerToken);
        
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            log.debug("추출된 토큰 길이: {}", token.length());
            return token;
        }
        
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // 다음 경로들은 JWT 인증을 건너뜀
        boolean shouldNotFilter = path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/actuator") ||
               path.equals("/") ||
               path.equals("/error") ||
               path.equals("/favicon.ico") ||
               path.startsWith("/auth/login") ||
               path.startsWith("/auth/register") ||
               path.equals("/auth/refresh") ||
               path.startsWith("/api/auth/kakao") ||
               path.startsWith("/oauth2") ||
               path.startsWith("/login/oauth2") ||
               path.equals("/kakao-callback.html") ||
               path.startsWith("/admin");
        
        log.debug("JWT 필터 건너뛰기: {} = {}", path, shouldNotFilter);
        return shouldNotFilter;
    }
} 