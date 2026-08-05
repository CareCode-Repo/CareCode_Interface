package com.carecode.domain.user.entity;

/** 동의 항목. 필수 항목은 미동의 시 서비스 이용이 불가하고, 선택 항목은 언제든 철회할 수 있다. */
public enum ConsentType {

    TERMS_OF_SERVICE("서비스 이용약관", true),
    PRIVACY_POLICY("개인정보 수집·이용", true),
    CHILD_DATA("자녀 정보 수집에 대한 보호자 동의", true),
    MARKETING("마케팅 정보 수신", false),
    THIRD_PARTY_SHARING("제3자 정보 제공", false);

    private final String displayName;
    private final boolean required;

    ConsentType(String displayName, boolean required) {
        this.displayName = displayName;
        this.required = required;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isRequired() {
        return required;
    }
}
