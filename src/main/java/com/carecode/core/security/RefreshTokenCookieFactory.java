package com.carecode.core.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * 리프레시 토큰을 HttpOnly 쿠키로 주고받기 위한 헬퍼.
 *
 * <p>리프레시 토큰을 응답 본문으로만 내리면 클라이언트가 JS 로 접근 가능한 저장소
 * (localStorage 등)에 둘 수밖에 없어 XSS 한 번에 세션 전체가 탈취된다.
 * HttpOnly 쿠키로 내려 스크립트가 읽지 못하게 하고, 경로를 {@code /auth} 로 좁혀
 * 일반 API 요청에는 실려 나가지 않도록 한다.
 *
 * <p>본문 응답도 당분간 유지한다. 쿠키를 쓸 수 없는 클라이언트(모바일 네이티브 등)와
 * 기존 웹 클라이언트가 함께 동작해야 하기 때문이다.
 */
@Component
public class RefreshTokenCookieFactory {

    public static final String COOKIE_NAME = "refreshToken";

    /** 쿠키가 실려 나갈 경로. 갱신·로그아웃 외의 요청에는 붙지 않는다. */
    private static final String COOKIE_PATH = "/auth";

    private final boolean secure;
    private final String sameSite;
    private final Duration maxAge;

    public RefreshTokenCookieFactory(
            @Value("${app.auth.refresh-cookie.secure:true}") boolean secure,
            @Value("${app.auth.refresh-cookie.same-site:None}") String sameSite,
            @Value("${app.auth.refresh-cookie.max-age-days:14}") long maxAgeDays) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.maxAge = Duration.ofDays(maxAgeDays);
    }

    /** 로그인·갱신 성공 시 내려보낼 쿠키. */
    public ResponseCookie create(String refreshToken) {
        return baseBuilder(refreshToken).maxAge(maxAge).build();
    }

    /** 로그아웃 시 즉시 만료시킬 쿠키. */
    public ResponseCookie expire() {
        return baseBuilder("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseBuilder(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(COOKIE_PATH);
    }

    /** 요청에 실려 온 리프레시 토큰을 꺼낸다. */
    public Optional<String> read(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }
}
