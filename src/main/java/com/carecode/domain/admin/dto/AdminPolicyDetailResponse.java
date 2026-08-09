package com.carecode.domain.admin.dto;

import com.carecode.domain.policy.entity.Policy;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 어드민 정책 목록·상세 응답.
 *
 * <p>사용자용 {@code PolicyDto} 는 화면 표시에 맞춰 값을 가공한다
 * (신청 기간을 "2026.01.01 ~ 2026.03.31" 문자열로 합치고, policyCode 는 아예 내려주지 않는다).
 * 그 값으로는 수정 화면을 채울 수 없다. {@code PolicyAdminService#apply} 는 요청에 담긴 값으로
 * 전체를 덮어쓰므로, 되돌려 보내지 못하는 필드는 수정할 때마다 null 이 된다.
 *
 * <p>그래서 어드민에는 {@link com.carecode.domain.admin.dto.AdminPolicyRequest} 와 1:1 로 대응하는
 * 원본 값을 내려준다. 사용자용 DTO 는 그대로 두어 화면 계약을 건드리지 않는다.
 */
@Getter
@Builder
public class AdminPolicyDetailResponse {

    private final Long id;

    // ==================== 수정 요청과 1:1 대응 ====================

    private final String policyCode;
    private final String title;
    private final String description;
    private final String policyType;
    private final Integer targetAgeMin;
    private final Integer targetAgeMax;
    private final String targetRegion;
    private final Integer benefitAmount;
    private final String benefitType;
    private final LocalDate applicationStartDate;
    private final LocalDate applicationEndDate;
    private final LocalDate policyStartDate;
    private final LocalDate policyEndDate;
    private final String applicationUrl;
    private final String contactInfo;
    private final String requiredDocuments;
    private final Boolean isActive;
    private final Integer priority;
    private final Long policyCategoryId;

    // ==================== 참고용 (수정 대상 아님) ====================

    private final String policyCategoryName;

    /** 금액이 수기 검증된 시각. null 이면 자동 수집된 추정치다. */
    private final LocalDateTime verifiedAt;
    private final String verifiedBy;
    private final String sourceUrl;

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static AdminPolicyDetailResponse from(Policy policy) {
        return AdminPolicyDetailResponse.builder()
                .id(policy.getId())
                .policyCode(policy.getPolicyCode())
                .title(policy.getTitle())
                .description(policy.getDescription())
                .policyType(policy.getPolicyType())
                .targetAgeMin(policy.getTargetAgeMin())
                .targetAgeMax(policy.getTargetAgeMax())
                .targetRegion(policy.getTargetRegion())
                .benefitAmount(policy.getBenefitAmount())
                .benefitType(policy.getBenefitType())
                .applicationStartDate(policy.getApplicationStartDate())
                .applicationEndDate(policy.getApplicationEndDate())
                .policyStartDate(policy.getPolicyStartDate())
                .policyEndDate(policy.getPolicyEndDate())
                .applicationUrl(policy.getApplicationUrl())
                .contactInfo(policy.getContactInfo())
                .requiredDocuments(policy.getRequiredDocuments())
                .isActive(policy.getIsActive())
                .priority(policy.getPriority())
                .policyCategoryId(policy.getPolicyCategory() != null
                        ? policy.getPolicyCategory().getId() : null)
                .policyCategoryName(policy.getPolicyCategory() != null
                        ? policy.getPolicyCategory().getName() : null)
                .verifiedAt(policy.getVerifiedAt())
                .verifiedBy(policy.getVerifiedBy())
                .sourceUrl(policy.getSourceUrl())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}
