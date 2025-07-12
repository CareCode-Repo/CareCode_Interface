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
    
    @Column(name = "target_age_min")
    private Integer targetAgeMin;
    
    @Column(name = "target_age_max")
    private Integer targetAgeMax;
    
    @Column(name = "target_region")
    private String targetRegion;
    
    @Column(name = "benefit_amount")
    private Integer benefitAmount;
    
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
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private PolicyCategory policyCategory;
    
    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PolicyDocument> policyDocuments = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 