package com.carecode.core.client.sync;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 보육통합정보 API 응답 코드 (명세서 v1.0 기준).
 * 한도 초과·키 만료를 "검색결과 없음" 과 구분하지 않으면 동기화가 조용히 0건으로 끝난다.
 */
public enum ChildcareApiStatus {

    OK(null, false),
    MISSING_PARAM("ERROR-100", false),
    SERVER_ERROR("ERROR-200", false),

    /** 아래 셋은 지역을 더 돌아도 소용없으므로 즉시 중단해야 한다. */
    INVALID_KEY("INFO-100", true),
    QUOTA_EXCEEDED("INFO-300", true),
    EXPIRED_KEY("INFO-400", true),

    /** 그 지역에 데이터가 없는 정상 상태. */
    NO_RESULT("INFO-200", false);

    private final String code;
    private final boolean fatal;

    ChildcareApiStatus(String code, boolean fatal) {
        this.code = code;
        this.fatal = fatal;
    }

    /** 계속 호출해도 의미가 없는 상태인지. */
    public boolean isFatal() {
        return fatal;
    }

    public String getCode() {
        return code;
    }

    public static ChildcareApiStatus of(JsonNode root) {
        if (root == null) {
            return OK;
        }
        JsonNode errcode = root.path("errcode");
        if (errcode.isMissingNode() || errcode.isNull()) {
            return OK;
        }
        String value = errcode.asText().trim();
        for (ChildcareApiStatus status : values()) {
            if (status.code != null && status.code.equals(value)) {
                return status;
            }
        }
        return SERVER_ERROR;
    }

    /** 사용자에게 보여줄 중단 사유. */
    public String describe(JsonNode root) {
        String message = root != null ? root.path("errmsg").asText("") : "";
        return message.isBlank() ? name() : code + " " + message;
    }
}
