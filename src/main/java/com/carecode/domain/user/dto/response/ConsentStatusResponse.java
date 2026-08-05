package com.carecode.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/** 항목별 현재 동의 상태. */
@Getter
@Builder
public class ConsentStatusResponse {

    private final List<ConsentItem> consents;

    @Getter
    @Builder
    public static class ConsentItem {
        private final String consentType;
        private final String displayName;
        private final boolean required;
        private final boolean granted;
        private final String policyVersion;
        private final LocalDateTime updatedAt;
    }

    @Getter
    @Builder
    public static class ConsentHistoryItem {
        private final String consentType;
        private final String displayName;
        private final boolean granted;
        private final String policyVersion;
        private final LocalDateTime createdAt;
    }
}
