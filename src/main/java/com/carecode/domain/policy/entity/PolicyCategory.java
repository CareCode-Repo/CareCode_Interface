package com.carecode.domain.policy.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 정책 카테고리 엔티티
 * 
 * @author CareCode Team
 * @since 1.0.0
 * @see Policy
 */
@Entity
@Table(name = "TBL_POLICY_CATEGORIES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PolicyCategory {

    /**
     * 카테고리 고유 식별자
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 카테고리명
     */
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    /**
     * 카테고리 설명
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 표시 순서
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    /**
     * 활성 상태 여부
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 생성 일시
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 이 카테고리에 속한 정책 목록
     */
    @OneToMany(mappedBy = "policyCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Policy> policies = new ArrayList<>();

    /**
     * 정책 카테고리 생성자
     * @param name 카테고리명 (필수, 고유해야 함)
     * @param description 카테고리 설명 (선택사항)
     * @param displayOrder 표시 순서 (기본값: 0)
     * 
     * @throws IllegalArgumentException name이 null이거나 빈 문자열인 경우
     */
    @Builder
    public PolicyCategory(String name, String description, Integer displayOrder) {
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    /**
     * 카테고리 정보 업데이트
     * @param name 새로운 카테고리명 (null이면 기존 값 유지)
     * @param description 새로운 카테고리 설명 (null이면 기존 값 유지)
     * @param displayOrder 새로운 표시 순서 (null이면 기존 값 유지)
     * 
     * @throws IllegalArgumentException name이 null이거나 빈 문자열인 경우
     */
    public void updateCategory(String name, String description, Integer displayOrder) {
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    /**
     * 카테고리 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 카테고리 활성화
     */
    public void activate() {
        this.isActive = true;
    }
} 