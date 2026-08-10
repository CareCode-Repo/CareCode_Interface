package com.carecode.domain.user.entity;

/** 동의 항목. 필수 항목은 미동의 시 서비스 이용이 불가하고, 선택 항목은 언제든 철회할 수 있다. */
public enum ConsentType {

    TERMS_OF_SERVICE("서비스 이용약관", true, false),
    PRIVACY_POLICY("개인정보 수집·이용", true, false),
    CHILD_DATA("자녀 정보 수집에 대한 보호자 동의", true, false),

    /**
     * 건강·의료 정보는 개인정보보호법상 민감정보라 일반 개인정보 동의로 갈음할 수 없다.
     * 키·몸무게·접종이력·진료기록을 다루므로 반드시 별도로 받는다.
     */
    HEALTH_DATA("건강정보 수집·이용 (민감정보)", false, true),

    MARKETING("마케팅 정보 수신", false, false),
    THIRD_PARTY_SHARING("제3자 정보 제공", false, false);

    private final String displayName;
    private final boolean required;
    private final boolean sensitive;

    ConsentType(String displayName, boolean required, boolean sensitive) {
        this.displayName = displayName;
        this.required = required;
        this.sensitive = sensitive;
    }

    /** 민감정보 여부. 별도 동의가 필요하고 철회 시 해당 기능을 즉시 막아야 한다. */
    public boolean isSensitive() {
        return sensitive;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isRequired() {
        return required;
    }
}
