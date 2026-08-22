package com.carecode.core.security;

import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/** Resolves the authenticated user from SecurityContextHolder and the persistence layer */
@Slf4j
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

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /**
     * 경로에 실린 사용자 식별자가 로그인한 본인인지 확인하고, 본인이면 엔티티를 돌려준다.
     *
     * <p>경로 변수는 발급된 {@code userId}(문자열)일 수도 있고 DB PK 일 수도 있다.
     * 서비스 계층이 두 형태를 모두 받아 조회하므로 검증도 두 형태를 모두 인정한다.
     *
     * <p>남의 식별자를 넣었을 때 404 가 아니라 403 을 주는 이유는, 404 로 응답하면
     * "그 ID 는 존재하지 않는다"는 정보가 새어 계정 열거에 쓰이기 때문이다.
     */
    public User requireSelf(String pathUserId) {
        User current = requireCurrentUser();
        if (matches(current, pathUserId)) {
            return current;
        }
        log.warn("본인이 아닌 사용자 자원 접근 시도 - 요청자={}, 대상={}", current.getUserId(), pathUserId);
        throw new CareServiceException("FORBIDDEN", "본인의 정보만 조회·변경할 수 있습니다.");
    }

    /** 본인이거나 관리자면 통과. 관리 화면과 본인 화면이 같은 엔드포인트를 쓰는 경우에만 사용한다. */
    public User requireSelfOrAdmin(String pathUserId) {
        if (isAdmin()) {
            return requireCurrentUser();
        }
        return requireSelf(pathUserId);
    }

    private boolean matches(User current, String pathUserId) {
        if (pathUserId == null || pathUserId.isBlank()) {
            return false;
        }
        if (pathUserId.equals(current.getUserId())) {
            return true;
        }
        return current.getId() != null && pathUserId.equals(String.valueOf(current.getId()));
    }
}
