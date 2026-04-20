package com.carecode.domain.policy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 정책 북마크 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyBookmarkRequest {
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    @NotNull(message = "정책 ID는 필수입니다")
    private Long policyId;
}

