package com.carecode.domain.user.service.refreshtoken;

/**
 * 서버 측 리프레시 토큰 세션(회전·로그아웃 시 폐기).
 * {@code jwt.refresh-token.store=none} 이면 모두 무작동으로 JWT만 검증하는 기존 동작과 동일합니다.
 */
public interface RefreshTokenStore {

    void register(String userId, String refreshTokenJwt);

    /**
     * Redis 등에 등록된 활성 리프레시 토큰인지 확인합니다.
     */
    boolean isRegistered(String refreshTokenJwt, String userId);

    void remove(String refreshTokenJwt, String userId);

    void removeAllForUser(String userId);
}
