package com.carecode.core.security;

/**
 * 액세스 토큰에서 꺼낸, 인증 필터가 쓰는 값들.
 *
 * @param userId 가입 시 발급한 사용자 코드
 * @param email  principal 로 쓰는 값
 * @param role   권한 문자열 (ROLE_ 접두사 없음)
 */
public record AccessTokenClaims(String userId, String email, String role) {
}
