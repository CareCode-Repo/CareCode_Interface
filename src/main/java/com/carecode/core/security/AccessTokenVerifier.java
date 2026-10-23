package com.carecode.core.security;

import java.util.Optional;

/**
 * 인증 필터가 토큰을 확인하는 통로.
 *
 * <p>필터는 토큰이 유효한지와 거기서 꺼낸 몇 개의 값만 필요하다. 그런데 구현
 * ({@code JwtService})은 사용자 도메인에 있고 로그인 응답 DTO({@code TokenDto} 등)까지
 * 다루므로, 필터가 그 클래스를 직접 쓰면 <b>보안 기반 코드가 사용자 도메인의 API 응답 모양을
 * 알게 된다.</b> 필요한 만큼만 이 인터페이스로 끊는다.
 *
 * <p>토큰 발급은 여기 없다. 발급은 로그인 흐름의 일부이고 응답 DTO 를 만들어야 하므로
 * 도메인에 남는다.
 */
public interface AccessTokenVerifier {

    /**
     * 유효한 액세스 토큰이면 담긴 값을 돌려준다.
     *
     * <p>만료·서명 불일치·토큰 종류 불일치는 모두 {@link Optional#empty()} 다. 필터는 그 셋을
     * 구분할 필요가 없고, 구분해서 응답에 담으면 공격자에게 단서를 준다.
     */
    Optional<AccessTokenClaims> verifyAccessToken(String token);
}
