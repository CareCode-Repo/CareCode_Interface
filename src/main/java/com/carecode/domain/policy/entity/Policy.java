package com.carecode.domain.policy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 육아 정책 엔티티
 * 정부에서 제공하는 육아 관련 정책 정보를 관리
 */
@Entity
@Table(name = "TBL_POLICIES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "policy_code", nullable = false, unique = true)
    private String policyCode;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "policy_type")
    private String policyType;
    
    /** 대상 연령 하한(개월). 세 단위 표기는 AgeRangeParser 로 환산해 넣는다. */
    @Column(name = "target_age_min")
    private Integer targetAgeMin;

    /** 대상 연령 상한(개월). null 이면 상한 없음. */
    @Column(name = "target_age_max")
    private Integer targetAgeMax;
    
    @Column(name = "target_region")
    private String targetRegion;

    /** 기준중위소득 대비 상한(%). null 이면 소득 무관 정책이다. */
    @Column(name = "income_threshold_percent")
    private Integer incomeThresholdPercent;

    /** 최소 자녀 수 요건. null 이면 무관, 2 이상이면 다자녀 정책이다. */
    @Column(name = "min_children")
    private Integer minChildren;

    /** 대상 연령이 지난 뒤에도 신청 가능한 개월 수. null 이면 소급 불가. */
    @Column(name = "retroactive_months")
    private Integer retroactiveMonths;
    
    @Column(name = "benefit_amount")
    private Integer benefitAmount;
    
    /**
     * 월 지급 정책의 최대 지급 개월. null 이면 대상 연령 구간 내내 지급한다.
     * 대상 연령과 지급 기간은 다르다 — 육아휴직급여는 아이가 0~96개월이어도 최대 12개월만 받는다.
     */
    @Column(name = "max_payment_months")
    private Integer maxPaymentMonths;

    @Column(name = "benefit_type")
    private String benefitType;
    
    @Column(name = "application_start_date")
    private LocalDate applicationStartDate;
    
    @Column(name = "application_end_date")
    private LocalDate applicationEndDate;
    
    @Column(name = "policy_start_date")
    private LocalDate policyStartDate;
    
    @Column(name = "policy_end_date")
    private LocalDate policyEndDate;
    
    @Column(name = "application_url")
    private String applicationUrl;
    
    @Column(name = "contact_info")
    private String contactInfo;
    
    @Column(name = "required_documents")
    private String requiredDocuments;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "priority")
    private Integer priority;

    @Column(name = "view_count")
    private Integer viewCount;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private PolicyCategory policyCategory;
    
    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    @lombok.Builder.Default
    private List<PolicyDocument> policyDocuments = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (viewCount == null) {
            viewCount = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 