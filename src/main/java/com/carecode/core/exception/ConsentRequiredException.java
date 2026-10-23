package com.carecode.core.exception;

/**
 * 동의가 없어 막혔음을 클라이언트가 구분할 수 있게 하는 예외.
 *
 * <p>전역 예외 핸들러가 이것을 잡아 "어떤 동의가 필요한가" 를 응답에 담는다. 그래서 동의 종류를
 * <b>문자열로</b> 들고 있다 — 열거형을 들고 있으면 {@code core/exception} 과 핸들러가 모두
 * 사용자 도메인의 {@code ConsentType} 을 알아야 한다. 응답은 JSON 이라 어차피 문자열이고,
 * 종류를 추가하는 일은 도메인 쪽에서만 일어난다.
 *
 * <p>한때 이 예외는 {@code ConsentGuard} 안의 중첩 클래스였고, 그래서 <b>웹 계층이 사용자
 * 도메인의 서비스를 import</b> 해야 예외를 가리킬 수 있었다.
 *
 * @see com.carecode.domain.user.service.ConsentGuard
 */
public class ConsentRequiredException extends CareServiceException {

    private final String consentType;
    private final String displayName;
    private final boolean sensitive;

    public ConsentRequiredException(String consentType, String displayName, boolean sensitive) {
        super(displayName + " 동의가 필요합니다.");
        this.consentType = consentType;
        this.displayName = displayName;
        this.sensitive = sensitive;
    }

    /** 동의 종류 코드. 클라이언트가 어떤 동의 화면을 띄울지 고르는 값이다. */
    public String getConsentType() {
        return consentType;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 민감정보 동의인지. 화면이 안내 문구를 다르게 띄운다. */
    public boolean isSensitive() {
        return sensitive;
    }
}
