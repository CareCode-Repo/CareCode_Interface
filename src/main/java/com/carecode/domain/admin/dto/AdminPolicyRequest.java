package com.carecode.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 관리자 정책 생성·수정 요청.
 *
 * <p>정책은 매년 바뀌는데 코드에 하드코딩돼 있어 재배포 없이는 수정할 수 없었다.
 * 이 API 로 운영 중 관리할 수 있게 한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminPolicyRequest {

    @NotBlank(message = "정책 코드는 필수입니다")
    @Size(max = 50)
    private String policyCode;

    @NotBlank(message = "정책명은 필수입니다")
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    @Size(max = 50)
    private String policyType;

    private Integer targetAgeMin;
    private Integer targetAgeMax;

    @Size(max = 100)
    private String targetRegion;

    private Integer benefitAmount;

    @Size(max = 50)
    private String benefitType;

    private LocalDate applicationStartDate;
    private LocalDate applicationEndDate;
    private LocalDate policyStartDate;
    private LocalDate policyEndDate;

    @Size(max = 500)
    private String applicationUrl;

    @Size(max = 200)
    private String contactInfo;

    @Size(max = 1000)
    private String requiredDocuments;

    private Boolean isActive;

    private Integer priority;

    /** 연결할 정책 카테고리 ID. */
    private Long policyCategoryId;
}
