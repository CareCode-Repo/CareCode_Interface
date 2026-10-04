package com.carecode.domain.user.dto.response;

/**
 * 카카오 인가 페이지 주소.
 *
 * <p>{@code redirect_uri} 는 서버 설정({@code KAKAO_REDIRECT_URI})을 쓰므로 프런트가 만들지 않는다.
 * 주소를 프런트가 조립하면 설정과 어긋날 때 카카오 쪽에서 막힌다.
 */
public record KakaoLoginUrlResponse(boolean success, String loginUrl, String message) {

    public static KakaoLoginUrlResponse of(String loginUrl) {
        return new KakaoLoginUrlResponse(true, loginUrl, "카카오 로그인 URL이 생성되었습니다.");
    }
}
