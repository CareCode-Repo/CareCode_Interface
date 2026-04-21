package com.carecode.core.security;

import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated user from {@link SecurityContextHolder} and the persistence layer.
 * Keeps controllers free of repeated SecurityContext + repository lookups.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserFacade {

    private final UserRepository userRepository;

    public String requireCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || "anonymousUser".equals(authentication.getName())) {
            throw new CareServiceException("UNAUTHORIZED", "인증 정보가 없습니다. 로그인 후 다시 시도하세요.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String s) {
            return s;
        }
        return authentication.getName();
    }

    public User requireCurrentUser() {
        String email = requireCurrentUserEmail();
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new CareServiceException("USER_NOT_FOUND", "인증 사용자를 찾을 수 없습니다."));
    }

    public String requireCurrentUserId() {
        return requireCurrentUser().getUserId();
    }

    public Long requireCurrentUserDbId() {
        return requireCurrentUser().getId();
    }
}
