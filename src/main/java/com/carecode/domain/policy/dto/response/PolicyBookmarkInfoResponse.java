package com.carecode.domain.policy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 정책 북마크 정보 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyBookmarkInfoResponse {
    private Long id;
    private String userId;
    private Long policyId;
    private LocalDateTime createdAt;
}

