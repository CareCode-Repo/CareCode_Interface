package com.carecode.domain.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 정책 부분 수정 요청.
 *
 * <p>{@link AdminPolicyRequest} 와 달리 필수 항목이 없다. 어느 필드를 실제로 바꿀지는
 * 이 객체가 아니라 <b>요청 JSON 에 그 키가 있었는지</b>로 판단한다
 * ({@code PolicyAdminService#patch}).
 *
 * <p>그래서 세 가지가 구분된다.
 * <ul>
 *   <li>키 없음 → 기존 값 유지</li>
 *   <li>키가 있고 값이 있음 → 그 값으로 변경</li>
 *   <li>키가 있고 값이 null → 값을 비움</li>
 * </ul>
 *
 * <p>null 만으로 판단하면 "비우기" 와 "건드리지 않기" 를 구분할 수 없어 둘 중 하나는 못 하게 된다.
 */
@Getter
@Setter
public class AdminPolicyPatchRequest {

    private String policyCode;
    private String title;
    private String description;
    private String policyType;
    private Integer targetAgeMin;
    private Integer targetAgeMax;
    private String targetRegion;
    private Integer benefitAmount;
    private String benefitType;
    private LocalDate applicationStartDate;
    private LocalDate applicationEndDate;
    private LocalDate policyStartDate;
    private LocalDate policyEndDate;
    private String applicationUrl;
    private String contactInfo;
    private String requiredDocuments;
    private Boolean isActive;
    private Integer priority;
    private Long policyCategoryId;
}
