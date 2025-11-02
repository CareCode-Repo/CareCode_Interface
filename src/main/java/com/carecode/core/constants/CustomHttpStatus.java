package com.carecode.core.constants;

/**
 * 맘편한 서비스 전용 커스텀 HTTP 상태코드 Enum
 * (Spring HttpStatus와 별도 관리)
 */
public enum CustomHttpStatus {
    CARE_POLICY_NOT_FOUND(460, "Care Policy Not Found"),
    CARE_FACILITY_NOT_FOUND(461, "Care Facility Not Found"),
    CHATBOT_TEMPORARILY_UNAVAILABLE(462, "Chatbot Temporarily Unavailable"),
    COMMUNITY_ANSWER_PENDING(463, "Community Answer Pending"),
    DUPLICATE_POLICY_APPLICATION(464, "Duplicate Policy Application"),
    ;

    private final int value;
    private final String reasonPhrase;

    CustomHttpStatus(int value, String reasonPhrase) {
        this.value = value;
        this.reasonPhrase = reasonPhrase;
    }

    public int value() {
        return value;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public static CustomHttpStatus valueOf(int statusCode) {
        for (CustomHttpStatus status : values()) {
            if (status.value == statusCode) {
                return status;
            }
        }
        throw new IllegalArgumentException("No matching constant for [" + statusCode + "]");
    }
} 