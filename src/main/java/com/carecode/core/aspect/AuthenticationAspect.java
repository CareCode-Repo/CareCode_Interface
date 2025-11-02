package com.carecode.core.aspect;

import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.exception.CareServiceException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * RequireAuthentication 어노테이션을 처리하는 Aspect
 * 인증이 필요한 API에 대한 인증 상태를 확인합니다.
 */
@Slf4j
@Aspect
@Component
public class AuthenticationAspect {

    /**
     * RequireAuthentication 어노테이션이 붙은 메서드 실행 전에 인증 상태를 확인
     */
    @Before("@annotation(requireAuthentication)")
    public void checkAuthentication(JoinPoint joinPoint, RequireAuthentication requireAuthentication) {
        log.debug("인증 상태 확인: {}", joinPoint.getSignature().getName());
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 인증되지 않은 경우
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getName())) {
            log.warn("인증되지 않은 사용자의 접근 시도: {}", joinPoint.getSignature().getName());
            throw new CareServiceException(requireAuthentication.message());
        }
        
        // 역할 기반 권한 확인 (필요한 경우)
        String[] requiredRoles = requireAuthentication.roles();
        if (requiredRoles.length > 0) {
            boolean hasRequiredRole = false;
            for (String role : requiredRoles) {
                if (authentication.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role))) {
                    hasRequiredRole = true;
                    break;
                }
            }
            
            if (!hasRequiredRole) {
                log.warn("권한이 없는 사용자의 접근 시도: {} (필요한 역할: {})", 
                    joinPoint.getSignature().getName(), String.join(", ", requiredRoles));
                throw new CareServiceException("접근 권한이 없습니다.");
            }
        }
        
        log.debug("인증 확인 완료: {}", authentication.getName());
    }
}
